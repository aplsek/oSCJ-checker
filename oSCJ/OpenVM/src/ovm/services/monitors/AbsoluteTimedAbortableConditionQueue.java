/**
 * AbsoluteTimedAbortableConditionQueue.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/monitors/AbsoluteTimedAbortableConditionQueue.java,v 1.3 2006/04/08 21:08:07 baker29 Exp $
 *
 */
package ovm.services.monitors;


/**
 * Extends {@link TimedAbortableConditionQueue} with methods that specify an
 * absolute deadline instead of a relative timeout.
 * @author David Holmes
 *
 */
public interface AbsoluteTimedAbortableConditionQueue 
    extends TimedAbortableConditionQueue {

    /**
     * Constant to reflect returning from an absolute timed wait immediately,
     * due to the deadline being in the past
     */
    static final int DEADLINE_PASSED = 3;

    /**
     * Causes the current thread to wait upon the condition, atomically
     * releasing the specified monitor in the process, until it is signalled,
     * aborted or the specified deadline has passed.
     * The thread can return from the wait under four conditions:
     * <ol>
     * <li>it was selected by a call to {@link ConditionQueueSignaller#signal}
     * <li>{@link ConditionQueueSignaller#signalAll} was invoked
     * <li>the specified deadline elapses
     * <li>it was the target of a call to {@link #abortWait}
     * </ol>
     * <p>When this method returns it is guaranteed that the current thread
     * owns the monitor.
     * <p>If the specified deadline has already passed then this method will
     * return immediately. It is not specified whether checking of the deadline
     * occurs before or after releasing of the monitor.
     * @return {@link TimedAbortableConditionQueue#SIGNALLED} if the wait 
     * completed due to a signal.
     *         {link #DEADLINE_PASSED} if the wait completed because the
     * specified deadline had already passed and no waiting occurred at all.
     *         {@link TimedAbortableConditionQueue#TIMED_OUT} if the wait 
     * completed due to the deadline elapsing.
     *         {@link TimedAbortableConditionQueue#ABORTED} if the wait 
     * completed due to a call to {@link #abortWait}.
     *
     * @param monitor the monitor to be released and then re-entered
     * @param deadline the absolute deadline in nanoseconds
     * @throws IllegalMonitorStateException if the monitor is not owned by the
     * current thread.
     */
    int waitAbsoluteTimedAbortable(Monitor monitor, long deadline);

}



















