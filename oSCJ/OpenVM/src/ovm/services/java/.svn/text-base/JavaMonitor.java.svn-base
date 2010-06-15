/**
 * JavaMonitor.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/java/JavaMonitor.java,v 1.8 2004/02/20 08:48:41 jthomas Exp $
 *
 */
package ovm.services.java;

import ovm.services.monitors.AbortableConditionQueue;
import ovm.services.monitors.ConditionQueueSignaller;
import ovm.services.monitors.Monitor;
import ovm.services.monitors.QueryableMonitor;
import ovm.services.monitors.RecursiveMonitor;
import ovm.services.monitors.TimedAbortableConditionQueue;
import ovm.util.Ordered;
/**
 * Defines the methods and semantics of a Java language monitor, complete
 * with its single implicit condition queue.
 *
 * This interfaces combines the applicable generic monitor and condition
 * queue interfaces from the {@link ovm.services.monitors} package and
 * refines the specifications therein to conform to the semantics of
 * Java language monitors as defined by the use of <code>synchronized</code>
 * methods and statements, and the methods {@link java.lang.Object#wait},
 * {@link java.lang.Object#notify}, {@link java.lang.Object#notifyAll}, and
 * {@link java.lang.Thread#interrupt}.
 *
 * @author David Holmes
 *
 * @see java.lang.Object#wait
 * @see java.lang.Object#notifyAll
 * @see java.lang.Object#notify
 * @see java.lang.Thread#interrupt
 *
 */
public interface JavaMonitor 
    extends Monitor,            // basic monitor entry and exit
            QueryableMonitor,   // useful for recursion and Thread.holdsLock
            RecursiveMonitor,   // optmization methods for recursion
            ConditionQueueSignaller, // for notify, notifyALL
            AbortableConditionQueue, // interruptable wait
            TimedAbortableConditionQueue, // timed, interruptable wait
            Ordered
{

    /**
     * Causes the current thread to attempt entry of this monitor. If this
     * monitor is unowned then the call succeeds immediately, the current
     * thread becomes the owner and the entry count is set at one. 
     * If the current thread is already the owner 
     * then this is a recursive entry and the entry count is increased by one.
     * If the monitor is already owned by another thread, then the current
     * thread will block until it is granted entry to the monitor. The order
     * in which multiple threads waiting for a monitor are granted it, is
     * not specified.
     *
     */
    void enter();

    /**
     * Causes the current thread to attempt to release ownership of this
     * monitor. The entry count is first decremented and if it is zero then
     * the monitor is released. If the current thread is not the owner then
     * an {@link java.lang.IllegalMonitorStateException} is thrown.
     * <p>Whether releasing the monitor results in an unowned monitor, or
     * the monitor is handed to an entering thread (if any) is not specified.
     *
     * @throws IllegalMonitorStateException if the current thread is not the
     * monitor owner.
     *
     */
    void exit();

    /**
     * Causes the current thread to attempt entry of this monitor. 
     * The semantics of this method are same as those of {@link #enter}. This
     * method exists as an optimisation for when it is known by the
     * implementation that the current thread does already own the monitor.
     *
     */
    void enterRecursive();

    /**
     * Causes the current thread to attempt to release ownership of this
     * monitor.
     * The semantics of this method are same as those of {@link #exit}. This
     * method exists as an optimisation for when it is known by the
     * implementation that the current thread does already own the monitor.
     *
     */
    void exitRecursive();

    /**
     * Causes the current thread to wait upon this condition, atomically
     * releasing this monitor in the process. The current thread must own
     * this monitor. 
     * <p>The thread can return from the wait under three conditions:
     * <ol>
     * <li>it was selected by a call to {@link #signal}
     * <li>{@link #signalAll} was invoked
     * <li>it was the target of a call to {@link #abortWait}
     * </ol>
     * <p>When this method returns it is guaranteed that the current thread
     * owns this monitor.
     *
     * @return <code>true</code> if the wait ended due to either of the first
     * two conditions above, and <code>false</code> if the wait ended due to
     * a call to {@link #abortWait}
     *
     * @param ignored This operation always act upon the current monitor
     */
    boolean waitAbortable(Monitor ignored);


    /**
     * Causes the current thread to wait upon the condition, atomically
     * releasing the current monitor in the process. The current thread
     * must own this monitor.
     * <p>The thread can return from the wait under four conditions:
     * <ol>
     * <li>it was selected by a call to {@link #signal}
     * <li>{@link #signalAll} was invoked
     * <li>the specified timeout has elapsed
     * <li>it was the target of a call to {@link #abortWait}
     * </ol>
     * <p>When this method returns it is guaranteed that the current thread
     * owns the monitor.
     * <p>The timeout value must be greater than, or equal to zero. A zero
     * timeout means a balking response.
     *
     * @return {@link #SIGNALLED} if the wait completed due to a signal.
     *         {@link #TIMED_OUT} if the wait completed due to the timeout
     * elapsing, or the timeout was zero
     *         {@link #ABORTED} if the wait completed due to a call to
     * {@link #abortWait}.
     *
     * @param ignored This operation always act upon the current monitor
     * @param timeout the timeout in nanoseconds
     * @throws IllegalMonitorStateException if the current thread does not
     * own this monitor.
     */
    int waitTimedAbortable(Monitor ignored, long timeout);

    /**
     * Signals one thread waiting upon this condition, such that
     * the thread will be able to return from a wait once it has
     * re-entered the monitor. Which thread is signalled is not specified.
     * The current thread must own this monitor.
     *
     * @throws IllegalMonitorStateException if the current thread does not
     * own this monitor.
     *
     */
    void signal();

    /**
     * Signals all threads waiting upon this condition, such that
     * they will be able to return from a wait once they have
     * reentered the monitor. The order in which threads are signalled
     * is not specified.
     * The current thread must own this monitor.
     *
     * @throws IllegalMonitorStateException if the current thread does not
     * own this monitor.
     *
     */
    void signalAll();

    /**
     * Defines a factory method for creating JavaMonitors
     *
     */
    public interface Factory extends Monitor.Factory {
        /**
         * Return a new Java monitor instance.
         * @return a new Java monitor instance.
         */
        Monitor newInstance();

        /**
         * Return a new Java monitor instance.
         * @return a new Java monitor instance.
         */
        JavaMonitor newJavaMonitorInstance();

    }
}






