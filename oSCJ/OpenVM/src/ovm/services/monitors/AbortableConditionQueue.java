/**
 * AbortableConditionQueue.java
 *
 *
 */
package ovm.services.monitors;

import ovm.core.services.threads.OVMThread;

/**
 * Defines a {@link ConditionQueue} which supports aborting of the
 * waiting upon the condition. A thread waiting upon the condition can be
 * specified as the target of an {@link #abortWait} call. If that thread
 * was using an abortable wait point then it will return from that
 * wait point.
 *
 * @author David Holmes
 *
 */
public interface AbortableConditionQueue extends ConditionQueue{

    /**
     * Causes the current thread to wait upon the condition, atomically
     * releasing the specified monitor in the process. 
     * The thread can return from the wait under three conditions:
     * <ol>
     * <li>it was selected by a call to {@link ConditionQueueSignaller#signal}
     * <li>{@link ConditionQueueSignaller#signalAll} was invoked
     * <li>it was the target of a call to {@link #abortWait}
     * </ol>
     * <p>When this method returns it is guaranteed that the current thread
     * owns the monitor.
     *
     * @return <code>true</code> if the wait ended due to either of the first
     * two conditions above, and <code>false</code> if the wait ended due to
     * a call to {@link #abortWait}
     *
     * @param monitor the monitor to be released and then re-entered
     * @throws IllegalMonitorStateException if the monitor is not owned by the
     * current thread.
     */
    boolean waitAbortable(Monitor monitor);


    /**
     * Attempts to abort the wait of the specified thread on the condition.
     * If the thread is not waiting through an
     * {@link #waitAbortable abortable wait point} then nothing
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









