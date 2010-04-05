/**
 * TimedAbortableConditionQueue.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/monitors/TimedAbortableConditionQueue.java,v 1.9 2004/02/20 08:48:46 jthomas Exp $
 *
 */
package ovm.services.monitors;

import ovm.core.services.threads.OVMThread;

/**
 * Defines a {@link ConditionQueue} which supports both aborting of, and
 * timing out from a wait upon the condition.
 * <p>This interface defines a new wait point to allow different forms of
 * wait to be supported by one implementation and because we need a ternary
 * return value.
 * @see TimedConditionQueue
 * @author David Holmes
 *
 */
public interface TimedAbortableConditionQueue extends ConditionQueue{

    /**
     * Constant to reflect a return from a wait due to a signal
     */
    static final int SIGNALLED = 0;

    /**
     * Constant to reflect a return from a wait due to a timeout
     */
    static final int TIMED_OUT = 1;

    /**
     * Constant to reflect a return from a wait due to an abort
     */
    static final int ABORTED = 2;

    /**
     * Causes the current thread to wait upon the condition, atomically
     * releasing the specified monitor in the process. 
     * The thread can return from the wait under four conditions:
     * <ol>
     * <li>it was selected by a call to {@link ConditionQueueSignaller#signal}
     * <li>{@link ConditionQueueSignaller#signalAll} was invoked
     * <li>the specified timeout has elapsed
     * <li>it was the target of a call to {@link #abortWait}
     * </ol>
     * <p>When this method returns it is guaranteed that the current thread
     * owns the monitor.
     * <p>The timeout value must be greater than, or equal to zero. A zero
     * timeout has implementation specific semantics - it could mean a balking
     * response, or it could mean an infinite timeout.
     *
     * @return {@link #SIGNALLED} if the wait completed due to a signal.
     *         {@link #TIMED_OUT} if the wait completed due to the timeout
     * elapsing.
     *         {@link #ABORTED} if the wait completed due to a call to
     * {@link #abortWait}.
     *
     * @param monitor the monitor to be released and then re-entered
     * @param timeout the timeout in nanoseconds
     * @throws IllegalMonitorStateException if the monitor is not owned by the
     * current thread.
     */
    int waitTimedAbortable(Monitor monitor, long timeout);

    /**
     * Attempts to abort the wait of the specified thread on the condition.
     * If the thread is not waiting through an
     * {@link #waitTimedAbortable abortable wait point} then nothing
     * happens and <code>false</code> is returned.
     *
     * @param thread the thread whose wait should be aborted
     *
     * @return <code>true</code> if <code>thread</code> was waiting
     * on an abortable wait point and has now had that wait
     * aborted; and <code>false</code> otherwise.
     *
     */
    boolean abortWait(OVMThread thread);

}



















