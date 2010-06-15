/**
 * BasicConditionQueue.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/monitors/BasicConditionQueue.java,v 1.2 2004/02/20 08:48:46 jthomas Exp $
 *
 */
package ovm.services.monitors;


/**
 * Defines a {@link ConditionQueue} with the basic ability to perform a wait
 * that is neither abortable nor times out.
 *
 * @see Monitor
 * @see ConditionQueueSignaller
 * @see AbortableConditionQueue
 * @see TimedConditionQueue
 * @see TimedAbortableConditionQueue
 * 
 * @author David Holmes
 *
 */
public interface BasicConditionQueue extends ConditionQueue {

    /**
     * Causes the current thread to wait upon the condition, atomically
     * releasing the
     * specified monitor in the process. When this method returns the
     * monitor has been re-entered. The exact circumstances under which 
     * the thread will stop waiting depends upon the concrete condition
     * type, but in general the thread will return when either it is
     * selected by the {@link ConditionQueueSignaller#signal} method, or 
     * {@link ConditionQueueSignaller#signalAll} is invoked.
     *
     * @param monitor the monitor to be released and then re-entered
     * @throws IllegalMonitorStateException if the monitor is not owned by the
     * current thread.
     */
    void wait(Monitor monitor);
}



