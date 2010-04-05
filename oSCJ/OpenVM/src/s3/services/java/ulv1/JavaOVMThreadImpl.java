
package s3.services.java.ulv1;

import ovm.core.domain.Oop;
import ovm.core.services.threads.OVMThreadContext;
import ovm.services.java.JavaDispatcher;
import ovm.services.java.JavaMonitor;
import ovm.services.java.JavaOVMThread;
import ovm.services.monitors.ConditionQueue;
import ovm.services.monitors.Monitor;
import ovm.util.OVMError;
import s3.core.domain.S3JavaUserDomain;
import s3.services.threads.TimedSuspensionOVMThreadImpl;
import s3.util.PragmaInline;
import s3.util.PragmaNoPollcheck;
import ovm.core.domain.WildcardException;
import ovm.core.Executive;

/**
 * An implementation of {@link ovm.services.java.JavaOVMThread JavaOVMThread} 
 * supporting the {@link s3.util.queues.SingleLinkElement SingleLinkElement} 
 * and {@link s3.util.queues.SingleLinkDeltaElement} interfaces for use with a 
 * {@link ovm.services.java.JavaUserLevelThreadManager user-level thread 
 * manager}. 
 * We build on the basic support provided by the
 * {@link s3.services.threads.TimedSuspensionOVMThreadImpl 
 * TimedSuspensionOVMThreadImpl} class for sleeping. To that we add explicit
 * state tracking so that we can effect the appropriate semantics of the
 * {@link java.lang.Thread} class with regard to interruption etc.
 *
 * <p><b>NOTE:</b>We do not support thread suspension. An earlier idea to use
 * ultra-low priority to effect thread suspension has been abandoned because
 * it won't work with priority inheritance.
 *
 * <p>This class is not generally thread-safe. It is expected that the caller
 * ensures exclusive access to this thread - typically we are used by the
 * thread manager, indirectly via the dispatcher, and the dispatcher ensures
 * thread safety.
 *
 *
 * @author David Holmes
 *
 */
public class JavaOVMThreadImpl extends TimedSuspensionOVMThreadImpl
    implements JavaOVMThread {

    /** The name of this thread */
    protected String name = "<un-named>";

    /** 
     * The reason this thread returned from its current blocking call.
     * This field is set by the code that terminates a blocking call
     * prematurely, and then queried by the current thread when it
     * returns from the blocking call. The current thread must reset this
     * to {@link JavaOVMThread#UNBLOCKED_NORMAL} if it finds it set to
     * anything else.
     */
    volatile int unblockState = UNBLOCKED_NORMAL;

    /** The Java Thread we are bound to */
    protected volatile Oop javaThread = null;

    /** The user-domain of the Java thread we are bound to */
    protected S3JavaUserDomain userDom;
    /** 
     * Current state of this thread. 
     */
    protected volatile int state = NOT_STARTED;

    /** The Java monitor upon which we are currently awaiting entry */
    volatile JavaMonitor waitMonitor = null;

    /** The Java monitor upon which we are currently waiting */
    volatile JavaMonitor waitCondition = null;
    
    /**
     * Our factory class for creating new threads
     */
    public static class Factory implements JavaOVMThread.Factory {

        public JavaOVMThread newInstance(Oop javaThread) {
            return new JavaOVMThreadImpl(javaThread);
        }
    }

    /**
     * Our factory instance. This is a per-class value and so must always
     * be accessed using the specific class name.
     */
    public static final JavaOVMThread.Factory factory = new Factory();

    /**
     * Construct a new OVM thread bound to the given Java thread.
     * When this thread starts executing it will execute the startup
     * method of the Java thread.    
     * @param javaThread the Java thread to which this thread should be bound
     * @throws OVMError.IllegalArgument if <code>javaThread</code> is 
     * <code>null</code>
     *
     */
    protected JavaOVMThreadImpl(Oop javaThread) {
        this.bindJavaThread(javaThread);
    }

    /**
     * Constructs an unbound thread. This thread can act as the
     * primordial thread.
     */
    protected JavaOVMThreadImpl(OVMThreadContext ctx) {
        super(ctx);
    }


    /**
     * Sets the state of this thread to <tt>READY</tt> and sets the initial
     * dynamic priority based on the bound Java thread's priority.
     * Note that we know that Java priorities map to OVM thread priorities
     * directly.
     */
    public void prepareForStart(JavaDispatcher _dispatcher) {
        assert getState() == NOT_STARTED:
	    "thread already started: " + getState();
        setState(READY);
        setPriority(getStartupPriority());
    }

    /** 
     * Reflectively retrieves the startup priority of the given thread.
     * This must be called atomically.
     */
    protected int getStartupPriority() {
	try {
	    return userDom.thread_startupPriority.call(javaThread).getInt();
	} catch (WildcardException e) {
	    throw Executive.panicOnException(e);
	}
    }

    /**
     * Invokes the startup method of the Java thread bound to this
     * OVM thread
     *
     */
    protected void doRun() {
        if (javaThread != null) {
            userDom.thread_runThread.call(javaThread);
            throw new OVMError("javaThread.runThread returned");
        }
        throw new OVMError("Trying to start an unbound thread");
    }


    /* Implementation methods for JavaOVMThread */

    public boolean isInterrupted() {
        return userDom.interrupted.get(getJavaThread());
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) {
            this.name = "<un-named>";
        }
        else {
            this.name = name;
        }
    }
            
    public Oop getJavaThread() throws PragmaInline {
        return javaThread;
    }

    /**
     * Binds this thread to the given Java thread. This is usually done
     * at construction time, but the primordial thread can only be bound
     * after creation.
     *
     * @throws OVMError.IllegalArgument if <tt>jThread</tt> was 
     * <tt>null</tt> and the current state is not <tt>TERMINATED</tt>.
     */
    public void bindJavaThread(Oop jThread) {
        if (jThread == null && state !=  TERMINATED){
            throw new OVMError.IllegalArgument("Thread was null");
        }
        if (jThread != null && javaThread != null) {
            throw new OVMError.IllegalState("thread already bound");
        }
        javaThread = jThread;
        userDom = (S3JavaUserDomain) jThread.getBlueprint().getDomain();
    }


    // warning: the set/get state methods are used by the sync code and so
    //          must not perform any sync internally. This means we have to 
    //          avoid allocation etc so no string concat or primitive-to-string
    //          conversions. -DH 18 Nov 2003

    public void setState(int state) throws PragmaNoPollcheck {
        assert (state == READY || state == TERMINATED ||
		state == BLOCKED_MONITOR || state == BLOCKED_WAIT ||
		state == BLOCKED_TIMEDWAIT || state == BLOCKED_SLEEP ||
		state == BLOCKED_SLEEP_NOINTERRUPT ||
		state == BLOCKED_IO):
	    "invalid thread state";
        this.state = state;
    }

    public int getState() throws PragmaNoPollcheck {
        return state;
    }

    public void setUnblockState(int state) throws PragmaNoPollcheck  {
        assert (state == UNBLOCKED_NORMAL || 
		state == UNBLOCKED_INTERRUPTED ||
		state == UNBLOCKED_TIMED_OUT ||
		state == UNBLOCKED_ASYNC_IO_CLOSE):
	    "invalid unblocking state";
        unblockState = state;
    }

    public int getUnblockState() throws PragmaNoPollcheck {
        return unblockState;
    }

    public void setWaitingMonitor(Monitor mon) 
        throws ovm.core.services.memory.PragmaNoBarriers, 
               PragmaNoPollcheck {
        if (waitMonitor != null && mon != null) {
            throw new OVMError.IllegalState("waiting monitor already set");
        }
        waitMonitor = (JavaMonitor)mon;
    }

    public Monitor getWaitingMonitor() throws PragmaNoPollcheck {
        return waitMonitor;
    }

    public void setWaitingConditionQueue(ConditionQueue mon) 
            throws ovm.core.services.memory.PragmaNoBarriers,
                   PragmaNoPollcheck {
        if (waitCondition != null && mon != null) {
            throw new OVMError.IllegalState("waiting condition already set");
        }
        waitCondition = (JavaMonitor)mon;
    }

    public ConditionQueue getWaitingConditionQueue() 
        throws PragmaNoPollcheck {
        return waitCondition;
    }


    /**
     * Suspension is not supported.
     * @return <tt>false</tt>
     */
    public boolean isSuspended() {
        return false;
    }

    /**
     * Suspension of threads is not currently supported.
     * @throws OVMError.UnsupportedOperation
     */
    public void setSuspended(boolean val) {
        throw new OVMError.UnsupportedOperation();
    }

}

    









