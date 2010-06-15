
package s3.core.services.events;

import ovm.core.execution.Native;
import ovm.core.execution.NativeInterface;
import ovm.core.services.events.EventManager;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.VM_Area;
import ovm.util.OVMError;
import s3.util.PragmaAtomic;
import s3.util.PragmaNoPollcheck;
import ovm.core.Executive;

/**
 *
 * @author Filip Pizlo, David Holmes
 */
public class EventManagerImpl
    extends ovm.services.ServiceInstanceImpl
    implements EventManager {
    
    /** the set of registered event processors
    */
    protected EventProcessor[] processors = new EventProcessor[10];
    protected int numProcessors = 0;
    
    /** the set of registered wait auditors
     */
    protected WaitingAuditor[] auditors = new WaitingAuditor[10];
    protected int numAuditors = 0;
    
    private static final class Helper implements NativeInterface {
        static final native void eventsSetEnabled(boolean enabled);
        static final native void waitForEvents();
    }
    
    private final static EventManager instance = new EventManagerImpl();
    
    public static EventManager getInstance() {
        return instance;
    }
    
    /**
     * The current enabled state
     */
    protected boolean enabled = false;

    /**
     * Are we started?
     */
    protected volatile boolean started = false;

    /**
     * Are we stopped?
     */
    protected volatile boolean stopped = false;

    /** 
     * Initialisation of the event manager simply involves grabbing
     * a thread manager.
     */
    public void init() {
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

    /**
     * Starts the event manager. This actually does nothing as the event
     * manager won't actually do anything until it is enabled, and that
     * must be done explicitly. But you must still call this as part of
     * the service instance protocol.
     * @throws OVMError.IllegalState {@inheritDoc}
     */
    public void start() {
        if (started) {
            throw new OVMError.IllegalState("event manager already started");
        }
        
        Native.makeSignalEventSimple();
	setupSignalEventFromThread();
        
        started = true;
	super.start();
        d("EventManager has been started");
    }
    
    protected void setupSignalEventFromThread() {
        Native.makeSignalEventFromThreadProper();
    }
    
    protected long standardWaitTime() {
	return -1;
    }

    public void stop() {
        if (!started) {
            return;
        }
        stopped = true;
        setEnabled(false);
    }

    public boolean isRunning() {
        return started & !stopped;
    }

    /**
     * @throws OVMError.IllegalState if the event manager has not been stopped
     */
    public void destroy() {
        if (isRunning()) {
            throw new OVMError.IllegalState("must stop event manager first");
        }
    }

    protected void callEventProcessor(int i)
	throws PragmaNoPollcheck {
	try {
	    processors[i].eventFired();
	}
	catch (StackOverflowError soe) {
	    // we handle this specially because the other debug code will
	    // likely retrigger a stackoverflow
	    Native.print_string("Warning: processEvents - stackoverflow has occurred\n");
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
		Native.print_string("Warning: processEvents - exception from eventProcessor[ ");
		Native.print_int(i);
		Native.print_string("] = ");
		Native.print_string(processors[i].toString());
		Native.print_string(": ");
		Native.print_string(t.toString());
		Native.print_string("\n");
	    }
	    catch (Throwable t2) {
		Native.print_string("\nWarning: processEvents - secondary exception\n");
	    }
	    finally {
		MemoryPolicy.the().leave(r1);
	    }
	}
    }

    // this is called with rescheduling disabled via the CSA
    public void processEvents() throws PragmaNoPollcheck {
        for (int i = 0; i < numProcessors; ++i) {
	    callEventProcessor(i);
        }
    }
    
    public void waitForEvents() throws PragmaNoPollcheck {
	long waitTime=standardWaitTime();
	for (int i=0;i<numAuditors;++i) {
	    waitTime=auditors[i].overrideWaitTime(waitTime);
	}
	if (waitTime<0) {
	    Helper.waitForEvents();
	} else {
	    Native.sched_yield();
	}
        processEvents();
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

    public boolean hasNonTrivialDumpStateHook() {
	return false;
    }

    public void dumpStateHook() {
	// do nothing
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

