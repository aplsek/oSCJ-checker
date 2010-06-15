package s3.services.events;

import ovm.core.Executive;
import ovm.core.execution.Native;
import ovm.core.execution.NativeConstants;
import ovm.core.execution.NativeInterface;
import ovm.core.services.events.EventManager;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.process.ForkManager;
import ovm.core.services.timer.TimerInterruptAction;
import ovm.core.services.timer.TimerManager;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.core.stitcher.TimerServicesFactory;
import ovm.services.io.signals.IOSignalManager;
import ovm.services.threads.UserLevelThreadManager;
import ovm.util.OVMError;
import s3.util.PragmaAtomic;
import s3.util.PragmaNoPollcheck;
/**
 *
 * @author Filip Pizlo
 */
public class EventAndIOManagerViaSelect
    extends ovm.services.ServiceInstanceImpl
    implements EventManager,
               IOSignalManager {
    
    public static final boolean DEBUG=false;
    
    /** the set of registered event processors
    */
    protected EventProcessor[] processors = new EventProcessor[10];
    private int numProcessors = 0;
    
    /** the set of registered wait auditors
     */
    protected WaitingAuditor[] auditors = new WaitingAuditor[10];
    protected int numAuditors = 0;
    
    private static final class Helper implements NativeInterface {
        
        static final native void eventsSetEnabled(boolean enabled);
        
        static final native int initSelect();
        static final native int resetPipeBecauseOfFork();
        static final native int doneSelect();
        
        // the given arrays must be FD_SETSIZE+1 in size.  after the call,
        // they contain a -1 deliminated list of file descriptors.
        static final native int doSelect(boolean block,
                                         int[] reads,
                                         int[] writes,
                                         int[] excepts);
        
        static final native int addSelectRead(int fd);
        static final native int delSelectRead(int fd);
        
        static final native int addSelectWrite(int fd);
        static final native int delSelectWrite(int fd);
        
        static final native int addSelectExcept(int fd);
        static final native int delSelectExcept(int fd);
        
        static final native void dumpSelectBits();
    }
    
    public static class CallbackNode {
        
        int fd;
        private Callback cback;
        void setCback(Callback c) throws PragmaNoBarriers {
            cback=c;
        }
        Callback getCback() throws PragmaNoBarriers {
            return cback;
        }
        
        CallbackNode prev=null;
        CallbackNode next=null;
        
        public CallbackNode(int fd,
                            Callback cback) {
            this.fd = fd;
            this.setCback(cback);
        }

    }
    
    private CallbackNode freeList_ = null;

    protected CallbackNode allocNode(int fd,Callback cback) {
        if (freeList_==null) {
            MemoryManager mm=MemoryManager.the();
            VM_Area prev=mm.setCurrentArea(mm.getImmortalArea());
            try {
                return new CallbackNode(fd,cback);
            } finally {
                mm.setCurrentArea(prev);
            }
        }
        CallbackNode result=freeList_;
        freeList_=result.next;
        
        result.fd=fd;
        result.setCback(cback);
        result.next=null;
        result.prev=null;
        
        return result;
    }
    
    protected void freeNode(CallbackNode n) {
        n.next=freeList_;
        n.prev=null;
        n.setCback(null);
        freeList_=n;
    }


    // Any reason a non-public class has public methods? - DH
    // Can we establish which methods only get called on an atomic path
    // (ie from waitForEvents or processEvents) and add throws 
    // PragmaNoPollcheck to all of them? - DH June 1, 2004
    
    abstract class OneModeSignalMan {
        private CallbackNode[] cbackForFd_ = new CallbackNode[NativeConstants.FD_SETSIZE];
	private String myName_;

	public OneModeSignalMan(String myName) {
	    this.myName_=myName;
	}
        
        public void dump() {
            for (int i=0;
                 i<cbackForFd_.length;
                 ++i) {
                if (cbackForFd_[i]==null) {
                    Native.print("0");
                } else {
                    Native.print("1");
                }
            }
        }
        
        public abstract void enableFD(int fd);
        public abstract void disableFD(int fd);
        
        public void start() {
            if (DEBUG) {
                Native.print_string("in ");
                Native.print_string(myName_);
                Native.print_string(".start()\n");
            }
            
            for (int i=0;i<cbackForFd_.length;++i) {
                if (!fdUsed(i)) {
                    continue;
                }
                if (DEBUG) {
                    Native.print_string("enabling ");
                    Native.print_int(i);
                    Native.print_string("\n");
                }
                enableFD(i);
            }
        }
    
        public void stop() {
            if (DEBUG) {
                Native.print_string("in ");
                Native.print_string(myName_);
                Native.print_string(".stop()\n");
            }
            
            for (int i=0;i<cbackForFd_.length;++i) {
                if (!fdUsed(i)) {
                    continue;
                }
                if (DEBUG) {
                    Native.print_string("disabling ");
                    Native.print_int(i);
                    Native.print_string("\n");
                }
                disableFD(i);
            }
        }

        public boolean fdUsed(int fd) {
            return cbackForFd_[fd]!=null;
        }
        
        public void addCallback(int fd,Callback cback) {
            if (DEBUG) {
                Native.print_string("For ");
                Native.print_string(myName_);
                Native.print_string(": adding cback for ");
                Native.print_int(fd);
                Native.print_string("\n");
            }

            if (!fdUsed(fd)) {
                enableFD(fd);
            }
            CallbackNode cbackNode=allocNode(fd,cback);
            cbackNode.next=cbackForFd_[fd];
            cbackNode.prev=null;
            if (cbackForFd_[fd]!=null) {
                cbackForFd_[fd].prev=cbackNode;
            }
            cbackForFd_[fd]=cbackNode;
        }

        private void removeCallbackImpl(CallbackNode cbackNode) {
            if (DEBUG) {
                Native.print_string("For ");
                Native.print_string(myName_);
                Native.print_string(": removing cback for ");
                Native.print_int(cbackNode.fd);
                Native.print_string(" [removeCallbackImpl]\n");
            }

            if (cbackNode.prev==null) {
                cbackForFd_[cbackNode.fd]=cbackNode.next;
                if (cbackNode.next!=null) {
                    cbackNode.next.prev=null;
                } else {
                    // now it's empty!
                    disableFD(cbackNode.fd);
                }
            } else {
                cbackNode.prev.next=cbackNode.next;
                if (cbackNode.next!=null) {
                    cbackNode.next.prev=cbackNode.prev;
                }
            }
            
            cbackNode.next=null;
            cbackNode.prev=null;
            
            freeNode(cbackNode);
        }
        
        protected void removeCallbackFromFDImpl(int fd,
                                                Callback cback,
                                                Object byWhat) {
            CallbackNode cur=cbackForFd_[fd];
            while (cur!=null) {
                CallbackNode next=cur.next;
                if (cur.getCback()==cback) {
                    cback.removed(byWhat);
                    removeCallbackImpl(cur);
                }
                cur=next;
            }
        }
        
        public void removeCallbackFromFD(int fd,
					 Callback cback,
					 Object byWhat) {
            removeCallbackFromFDImpl(fd,cback,byWhat);
        }
        
        public void removeCallback(Callback cback,
				   Object byWhat) {
            for (int i=0;i<cbackForFd_.length;++i) {
                removeCallbackFromFDImpl(i,cback,byWhat);
            }
        }
        
        public void removeFD(int fd,
			     Object byWhat) {
            if (DEBUG) {
                Native.print_string("For ");
                Native.print_string(myName_);
                Native.print_string(": removing cback for ");
                Native.print_int(fd);
                Native.print_string(" [removeFD]\n");
            }

            if (cbackForFd_[fd]==null) {
                return;
            }
            
            do {
                CallbackNode cur=cbackForFd_[fd];
                cur.getCback().removed(byWhat);
                cbackForFd_[fd]=cur.next;
                cur.next=null;
                cur.prev=null;
            } while (cbackForFd_[fd]!=null);
            
            disableFD(fd);
        }
        
        public void callIOSignalOnFd(int fd) {
            CallbackNode cur=cbackForFd_[fd];
            while (cur!=null) {
                if (cur.getCback().signal(true)) {
                    cur=cur.next;
                } else {
                    CallbackNode next=cur.next;
                    cur.getCback().removed(Callback.BY_SIGNAL);
                    removeCallbackImpl(cur);
                    cur=next;
                }
            }
        }
        
        // the given array is deliminated with a -1
        public void processSignals(int[] fds) throws PragmaNoPollcheck {
            for (int i=0;fds[i]>=0;++i) {
                if (!fdUsed(fds[i])) {
                    throw panic("Told to process signals on fd = "+fds[i]+
                                ", which we have no record of");
                }
                callIOSignalOnFd(fds[i]);
            }
        }
    }
    
    private final static EventAndIOManagerViaSelect instance =
        new EventAndIOManagerViaSelect();
    
    public static EventAndIOManagerViaSelect getInstance() {
        return instance;
    }
    
    /**
     * The current enabled state
     */
    protected boolean enabled = false;

    /**
     * Are we stopped?
     */
    protected volatile boolean stopped = false;

    /** 
     * Reference to the thread  manager being used 
     */
    protected UserLevelThreadManager tm;
    
    /**
     * reference to the timer manager
     */
    protected TimerManager timer;

    public boolean hasNonTrivialDumpStateHook() {
	return true;
    }
    
    public void dumpStateHook() {
	dump();
    }

    void dump() {
        Native.print("EventAndIOManagerViaSelect Dump:\n");
        Native.print("Java-land read: ");
        if (read==null) {
            Native.print("null\n");
        } else {
            read.dump();
            Native.print("\n");
        }
        Native.print("Java-land write: ");
        if (write==null) {
            Native.print("null\n");
        } else {
            write.dump();
            Native.print("\n");
        }
        Native.print("Java-land except: ");
        if (except==null) {
            Native.print("null\n");
        } else {
            except.dump();
            Native.print("\n");
        }
        Helper.dumpSelectBits();
    }
    
    Error panic(String message) {
        dump();
        return Executive.panic(message);
    }
    
    Error panicOnErrno(String message,int errno) {
        dump();
        return Executive.panicOnErrno(message,errno);
    }
    
    void check(String action,
               int result,
               int errno) {
        if (result<0) {
            throw panicOnErrno("Failed while doing "+action,errno);
        }
    }
    
    protected OneModeSignalMan read = new OneModeSignalMan("read") {
        public void enableFD(int fd) {
            //d("addSelectRead("+fd+")");
            check("addSelectRead",
                  Helper.addSelectRead(fd),
                  Native.getErrno());
        }
        public void disableFD(int fd) {
            //d("delSelectRead("+fd+")");
            check("delSelectRead",
                  Helper.delSelectRead(fd),
                  Native.getErrno());
        }
    };
    
    protected OneModeSignalMan write = new OneModeSignalMan("write") {
        public void enableFD(int fd) {
            //d("addSelectWrite("+fd+")");
            check("addSelectWrite",
                  Helper.addSelectWrite(fd),
                  Native.getErrno());
        }
        public void disableFD(int fd) {
            //d("delSelectWrite("+fd+")");
            check("delSelectWrite",
                  Helper.delSelectWrite(fd),
                  Native.getErrno());
        }
    };
    
    protected OneModeSignalMan except = new OneModeSignalMan("except") {
        public void enableFD(int fd) {
            //d("addSelectExcept("+fd+")");
            check("addSelectExcept",
                  Helper.addSelectExcept(fd),
                  Native.getErrno());
        }
        public void disableFD(int fd) {
            //d("delSelectExcept("+fd+")");
            check("delSelectExcept",
                  Helper.delSelectExcept(fd),
                  Native.getErrno());
        }
    };
    
    protected ForkManager.AfterHandler afterForkHandler =
        new ForkManager.AfterHandler() {
        public void afterInChild() {
            // need to reset the pipe, since otherwise it would be shared between
            // parent and child, which would lead to livelocks.
            check("resetPipeBecauseOfFork",
                  Helper.resetPipeBecauseOfFork(),
                  Native.getErrno());
        }
        public void afterInParent(int childPid) {
            // couldn't care less!
        }
    };

    public void init() {
        tm = (UserLevelThreadManager)
            ((ThreadServicesFactory)ThreadServiceConfigurator.config.
             getServiceFactory(ThreadServicesFactory.name)).getThreadManager();
        if (tm == null) {
            throw new OVMError.Configuration("need a configured thread manager");
        }
        
        timer = ((TimerServicesFactory)ThreadServiceConfigurator.config.
            getServiceFactory(TimerServicesFactory.name)).getTimerManager();
        if (timer == null) {
            throw new OVMError.Configuration("need a configured timer service");
        }

        isInited = true;
    }

    // warning: DO NOT perform any allocation in this method!!!! Use only
    // raw native I/O
    public boolean setEnabled(boolean enabled) throws PragmaNoPollcheck {
        boolean temp = this.enabled;
        this.enabled = enabled;
	if (temp!=enabled) {
	    Helper.eventsSetEnabled(enabled);
	}
        return temp;
    }

    public boolean isEnabled() { return enabled; }

    protected final TimerInterruptAction selecter = new TimerInterruptAction(){
	    public String timerInterruptActionShortName() {
		return "select";
	    }
	    public void fire(int ticks) throws PragmaNoPollcheck {
		// ticks is always >= 1
		doSelect(false);
	    }
	};

    /**
     * Starts the event manager. This actually does nothing as the event
     * manager won't actually do anything until it is enabled, and that
     * must be done explicitly. But you must still call this as part of
     * the service instance protocol.
     * @throws OVMError.IllegalState {@inheritDoc}
     */
    public void start() {
        if (isStarted) {
            throw new OVMError.IllegalState("event manager already started");
        }
        
        check("initSelect",
              Helper.initSelect(),
              Native.getErrno());
        
        read.start();
        write.start();
        except.start();
        
        timer.addTimerInterruptAction(selecter);
        
        ForkManager.addAfter(afterForkHandler);
        
        isStarted = true;
        
        d("EventAndIOManager has been started");
    }

    public void stop() {
        if (!isStarted || stopped) {
            return;
        }

        read.stop();
        write.stop();
        except.stop();

        check("doneSelect",
              Helper.initSelect(),
              Native.getErrno());
        
        ForkManager.removeAfter(afterForkHandler);

        stopped = true;

        setEnabled(false);
    }

    public boolean isRunning() {
        return isStarted & !stopped;
    }

    /**
     * @throws OVMError.IllegalState if the event manager has not been stopped
     */
    public void destroy() {
        if (isRunning()) {
            throw new OVMError.IllegalState("must stop event manager first");
        }
    }


    private void processEventsImpl() throws PragmaNoPollcheck {
        for (int i = 0; i < numProcessors; ++i) {
            try {
                processors[i].eventFired();
            }
            catch (StackOverflowError soe) {
                // we handle this specially because the other debug code will
                // likely retrigger a stackoverflow
                Native.print_string("Warning: processEventsImpl - stackoverflow has occurred\n");
                // normally we don't allow exceptions to propagate from
                // here but we make a special allowance for a stack
                // overflow. The fact that the stackoverflow occurred
                // may well have left the OVM internals in an inconsistent
                // state. - DH
                throw soe;
            }
            catch(Throwable t) {
                // This is debug code but watch for generating secondary
                // exceptions due to memory problems
                Object r1 = MemoryPolicy.the().enterExceptionSafeArea();
                try {
                    // Should log somewhere !!!
                    Native.print_string("Warning: processEventsImpl - exception from eventProcessor[ ");
                    Native.print_int(i);
                    Native.print_string("] = ");
                    Native.print_string(processors[i].toString());
                    Native.print_string(": ");
                    Native.print_string(t.toString());
                    Native.print_string("\n");
                    t.printStackTrace();
                }
                catch (Throwable t2) {
                    Native.print_string("\nWarning: processEventsImpl - secondary exception\n");
                }
                finally {
                    MemoryPolicy.the().leave(r1);
                }
            }
        }
    }
    
    private int[] readFds=new int[NativeConstants.FD_SETSIZE + 1];
    private int[] writeFds=new int[NativeConstants.FD_SETSIZE + 1];
    private int[] exceptFds=new int[NativeConstants.FD_SETSIZE + 1];
    private void doSelect(boolean block) throws PragmaNoPollcheck {
        Helper.doSelect(block,
                        readFds,
                        writeFds,
                        exceptFds);
        
        read.processSignals(readFds);
        write.processSignals(writeFds);
        except.processSignals(exceptFds);
    }

    // this is called with rescheduling disabled via the CSA
    public void processEvents() throws PragmaNoPollcheck {
        processEventsImpl();
    }
    
    public void waitForEvents() throws PragmaNoPollcheck {
	long waitTime=-1;
	for (int i=0;i<numAuditors;++i) {
	    waitTime=auditors[i].overrideWaitTime(waitTime);
	}
        doSelect(waitTime<0);
        processEventsImpl();
    }

    /** The actual implementation for adding an event processor and can be
        overridden by subclasses.
        This is only called with PragmaAtomic active.
    */
    protected int addEventProcessorImpl(EventProcessor handler) 
        throws PragmaNoPollcheck {
	int index=numProcessors;
	if (index == processors.length) {
	    throw Executive.panic("Too many event processors");
	}
	processors[index] = handler;
	numProcessors++;
	return index;
    }
    
    /**
     * {@inheritDoc}
     * <p>This method executes atomically and establishes the correct
     * allocation context in case <tt>addEventProcessImpl</tt> needs to
     * allocate
     */
    public final void addEventProcessor(EventProcessor handler)
	throws PragmaAtomic {
	addEventProcessorImpl(handler);
    }

    protected void removeEventProcessorHook(int i) throws PragmaNoPollcheck {
    }
    
    public final void removeEventProcessor(EventProcessor handler)
	throws PragmaAtomic {
	for (int i = 0; i < processors.length; ++i) {
	    if (processors[i] == handler) {
		removeEventProcessorHook(i);
		
		// fill in the gap
		processors[i] = processors[--numProcessors];
		// don't hold extra references
		processors[numProcessors] = null;
		break;
	    }
	}
    }
    
    
    public void addCallbackForRead(int fd,Callback cback)
	throws PragmaAtomic
    {
        read.addCallback(fd,cback);
    }
    
    public void addCallbackForWrite(int fd,Callback cback)
	throws PragmaAtomic
    {
        write.addCallback(fd,cback);
    }
    
    public void addCallbackForExcept(int fd,Callback cback)
	throws PragmaAtomic
    {
        except.addCallback(fd,cback);
    }
    
    public void removeCallbackFromFD(int fd,
				     Callback cback,
				     Object byWhat)
	throws PragmaAtomic
    {
        read.removeCallbackFromFD(fd,cback,byWhat);
        write.removeCallbackFromFD(fd,cback,byWhat);
        except.removeCallbackFromFD(fd,cback,byWhat);
    }
    
    public void removeCallback(Callback cback,
			       Object byWhat) {
        read.removeCallback(cback,byWhat);
        write.removeCallback(cback,byWhat);
        except.removeCallback(cback,byWhat);
    }
    
    public void removeFD(int fd,
			 Object byWhat)
	throws PragmaAtomic
    {
        read.removeFD(fd,byWhat);
        write.removeFD(fd,byWhat);
        except.removeFD(fd,byWhat);
    }

    public long delayedWaitResolution() {
	return -1;
    }
    
    public void addWaitingAuditor(WaitingAuditor wa)
	throws PragmaAtomic {
	if (numAuditors==auditors.length) {
	    throw Executive.panic("Too many waiting auditors");
	}
	auditors[numAuditors++]=wa;
    }
    
    public void removeWaitingAuditor(WaitingAuditor wa)
	throws PragmaAtomic {
	for (int i=0;i<numAuditors;++i) {
	    auditors[i]=auditors[--numAuditors];
	    auditors[numAuditors]=null;
	    break;
	}
    }

    
    public void resetProfileHistograms() {
    }
    
    public void disableProfileHistograms() {
    }
}

