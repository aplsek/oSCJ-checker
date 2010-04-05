/**
 * TimedConditionQueue.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/monitors/TimedConditionQueue.java,v 1.7 2004/02/20 08:48:47 jthomas Exp $
 *
 */
package ovm.services.monitors;


/**
 * Defines a {@link ConditionQueue} which supports a time-out on 
 * waiting upon the condition. 
 *
 * @author David Holmes
 *
 */
public interface TimedConditionQueue extends ConditionQueue {

    /**
     * Causes the current thread to wait upon the condition, atomically
     * releasing the specified monitor in the process. 
     * The thread can return from the wait under three conditions:
     * <ol>
     * <li>it was selected by a call to {@link ConditionQueueSignaller#signal}
     * <li>{@link ConditionQueueSignaller#signalAll} was invoked
     * <li>the specified timeout has elapsed
     * </ol>
     * <p>When this method returns it is guaranteed that the current thread
     * owns the monitor.
     * <p>The timeout value must be greater than, or equal to zero. A zero
     * timeout has implementation specific semantics - it could mean a balking
     * response, or it could mean an infinite timeout.
     *
     * @return <code>true</code> if the wait ended due to either of the first
     * two conditions above, and <code>false</code> if the wait ended due to
     * a time-out.
     *
     * @param monitor the monitor to be released and then re-entered
     * @param timeout the timeout in nanoseconds
     * @throws IllegalMonitorStateException if the monitor is not owned by the
     * current thread.
     */
    boolean waitTimed(Monitor monitor, long timeout);

}
















