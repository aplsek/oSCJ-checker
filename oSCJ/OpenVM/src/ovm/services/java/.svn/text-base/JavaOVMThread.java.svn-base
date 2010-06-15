/*
 * JavaOVMThread.java
 *
 * Created on November 28, 2001, 2:59 PM
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/java/JavaOVMThread.java,v 1.24 2007/05/18 17:42:40 baker29 Exp $
 */
package ovm.services.java;

import ovm.core.domain.Oop;
import ovm.services.monitors.ConditionQueue;
import ovm.services.monitors.Monitor;
import ovm.services.threads.ConditionQueueTrackingThread;
import ovm.services.threads.MonitorTrackingThread;
import ovm.services.threads.PriorityOVMThread;

/**
 * A kernel-domain thread that represents a {@link java.lang.Thread} in the
 * user-domain: there is a one-to-one association between them.
 * When the <code>JavaOVMThread</code> starts executing it
 * executes the start-up method of its associated {@link java.lang.Thread}
 * object. This interface provides methods and constants to make it easy
 * to enforce the semantics required by the {@link java.lang.Thread} API.
 *
 * <p>The different execution states of the thread are defined by constants. 
 * A thread is basically either ready (possibly running) or in one of the 
 * blocked states.
 * All blocking states will return non-zero when anded 
 * with {@link #BLOCKED_MASK}, while non blocking states will return zero.
 * Because a thread can be suspended while in any state, we don't define
 * a separate suspend state. Instead we have a general query method
 * {@link #isSuspended} that can be used to see if a thread is suspended.
 * The exact semantics of suspending a thread are implementation specific.
 *<p>As a <code>JavaOVMThread</code> can block for different reasons, there are
 * also constants defining why the thread unblocked. Implementations can use 
 * these constants together with the {@link #setUnblockState} and
 * {@link #getUnblockState} methods to help track why a thread was unblocked
 * and react accordingly.
 * <p>To deal with thread interruption (cancellation) a thread always stores
 * a reference to the {@link JavaMonitor} it is blocked on (either for entry or
 * on a wait). This can be retrieved using {@link #getWaitingMonitor}.
 *
 * <p><b>NOTE:</b> As this is a kernel-domain interface no user-domain types
 * are available. As an opaque type we use <tt>Oop</tt> where we really
 * expect <tt>java.lang.Thread</tt>.
 *
 * @see java.lang.Thread
 * @author  David Holmes
 * @version 1.0
 */
public interface JavaOVMThread 
    extends PriorityOVMThread, 
            MonitorTrackingThread, 
            ConditionQueueTrackingThread 
{

    /**
     * A factory interface for creating new Java OVM threads. The current
     * factory should be obtained from the configuration and then used by the
     * dispatcher to create a thread of the right concrete type.
     *
     */
    public interface Factory {

        /**
         * Construct a new Java OVM thread bound to the given Java thread.
         * When this thread starts executing it will execute the startup
         * method of the Java thread.
         * <p>The specifed Java thread should not already be bound.
         * @param javaThread the Java thread to which this OVM thread should 
         * be bound
         * @throws ovm.util.OVMError.IllegalArgument if <code>thread</code> is 
         * <code>null</code>
         *
         */
        JavaOVMThread newInstance(Oop javaThread);
    }

    /* Thread state constants */

    /**
     * Thread has not yet been started by a dispatcher
     */
    static final int NOT_STARTED = 0x1000;

    /**
     * Thread is currently reading to execute. It may actually be executing but
     * we can't tell that.
     */
    static final int READY = 0x0000;

    /**
     * Thread has terminated.
     */
    static final int TERMINATED = 0x2000;

    /**
     * Mask to see if thread is in any blocked state
     */
    static final int BLOCKED_MASK = 0x1000;

    /**
     * Thread is currently blocked entering a monitor
     */
    static final int BLOCKED_MONITOR = BLOCKED_MASK | 0x0001;

    /**
     * Thread is currently blocked doing a monitor wait
     */
    static final int BLOCKED_WAIT = BLOCKED_MASK | 0x0002;

    /**
     * Thread is currently blocked doing a timed monitor wait
     */
    static final int BLOCKED_TIMEDWAIT = BLOCKED_MASK | 0x0004;

    /**
     * Thread is currently blocked doing a sleep
     */
    static final int BLOCKED_SLEEP = BLOCKED_MASK | 0x0008;

    /**
     * Thread is currently blocked doing an uninterruptible sleep
     */
    static final int BLOCKED_SLEEP_NOINTERRUPT = BLOCKED_MASK | 0x0010;

    /**
     * Thread is currently blocked on an I/O call
     */
    static final int BLOCKED_IO = BLOCKED_MASK | 0x0020;
    

    /**
     * Set the current execution state of this thread. In some implementations
     * only the current thread may be able to set its own state.
     *
     * @param newState the new state of this thread
     */
    void setState(int newState);

    /**
     * Query the current state of this thread.
     *
     * @return the current state of this thread
     *
     */
    int getState();

    /**
     * Queries if this thread is suspended.
     * @return <code>true</code> if this thread is suspended and 
     * <code>false</code> otherwise
     *
     */
    boolean isSuspended();

    /**
     * Sets the suspend state of this thread
     * @param suspended the new value of the suspend state
     *
     */
    void setSuspended(boolean suspended);

    /* unblock constants */

    /** The thread unblocked because the operation completed normally */
    static final int UNBLOCKED_NORMAL = 0x0;

    /** The thread unblocked because it was interrupted */
    static final int UNBLOCKED_INTERRUPTED = 0x01;

    /** The thread unblocked due to a timeout of some form */
    static final int UNBLOCKED_TIMED_OUT = 0x02;

    /** The thread unblocked due to the asynchronous close of an I/O stream */
    static final int UNBLOCKED_ASYNC_IO_CLOSE = 0x04;

    /**
     * Informs this thread as to why a blocking operation unblocked
     * @param state the constant indicating why the thread unblocked
     * @see #getUnblockState
     */
    void setUnblockState(int state);

    /**
     * Returns the unblock state of this thread, as it was last set.
     * @return the unblock state of this thread, as it was last set.
     *
     */
    int getUnblockState();

    /**
     * Is the tread about to wake up and throw an
     * InterruptedException?
     **/
    boolean isInterrupted();

    /**
     * Returns an opaque reference to the {@link java.lang.Thread} object 
     * bound to this thread.
     * This value may be <code>null</code> if no
     * <code>Thread</code> object has yet been bound to this thread.
     *
     * @see #bindJavaThread
     * @return an opaque reference to the {@link java.lang.Thread} object 
     * bound to this thread.
     */
    Oop getJavaThread();
    
    /**
     * Binds the given {@link java.lang.Thread} object to this thread.
     * A thread may only be bound once. The exact time
     * of binding depends on the implementation but binding must occur before
     * the thread starts executing. Typically, a thread will be bound upon
     * creation but that may not be possible for the primordial thread.
     *
     * <p>The specifed Java thread should not already be bound.
     *
     * <p><b>Note:</b>This may be too restrictive. We could allow rebinding 
     * so that we can reuse the JavaOVMThread object, and perhaps its stack?
     *
     * @param javaThread Opaque reference to the {@link java.lang.Thread} 
     * object to bind to.
     *
     * @throws NullPointerException if <code>thread</code> is <code>null</code>
     * @throws ovm.util.OVMError.IllegalState if this thread has already been 
     * bound
     */
    void bindJavaThread(Oop javaThread);


    /**
     * Performs thread-specific state configuration in preparation for a thread
     * being started. This method is called by the dispatcher when a thread is
     * being started, and allows the thread to prepare any state (such as
     * initial priority) that might need to be prepared (such as querying the
     * bound Java thread object).
     *
     * @param dispatcher the Java dispatcher being used. This might be needed
     * obtain additional configuration information.
     */
    void prepareForStart(JavaDispatcher dispatcher);

    // override these definitions

    /**
     * Sets the {@link JavaMonitor} upon which this thread is awaiting entry.
     * This makes it easier to find a thread when it is interrupted, or to find
     * if a thread is in a queue that needs reordering when its priority 
     * changes.
     *
     * @param mon the monitor upon which this thread is awaiting entry, or 
     * <code>null</code> if this thread is no longer waiting upon any monitor.
     * @throws ovm.util.OVMError.IllegalState if <code>mon</code> is not 
     * <code>null</code> and this thread is already waiting on a monitor
     * @throws ClassCastException if <code>mon</code> is not a 
     * <code>JavaMonitor</code>
     */
    void setWaitingMonitor(Monitor mon);

    /**
     * Returns the {@link JavaMonitor} upon which this thread is awaiting 
     * entry.
     * @return the monitor upon which this thread is waiting, 
     * or <code>null</code> if this thread is not awaiting entry.
     *
     */
    Monitor getWaitingMonitor();

    /**
     * Sets the {@link JavaMonitor} upon which this thread is waiting.
     * This makes it easier to find a thread when it is interrupted, or to find
     * if a thread is in a queue that needs reordering when its priority 
     * changes.
     *
     * @param mon the monitor upon which this thread is waiting entry, or 
     * <code>null</code> if this thread is no longer waiting upon any monitor.
     * @throws ovm.util.OVMError.IllegalState if <code>mon</code> is not 
     * <code>null</code> and this
     * thread is already waiting on a monitor
     * @throws ClassCastException if <code>mon</code> is not 
     * a <code>JavaMonitor</code>
     */
    void setWaitingConditionQueue(ConditionQueue mon);

    /**
     * Returns the {@link JavaMonitor} upon which this thread is waiting.
     * @return the monitor upon which this thread is waiting, 
     * or <code>null</code> if this thread is not awaiting entry.
     *
     */
    ConditionQueue getWaitingConditionQueue();

    /**
     * Returns the name of this thread.
     */
    String getName();

    /**
     * Sets the name of this thread.
     * @param name the name to give the thread
     */
    void setName(String name);
}












