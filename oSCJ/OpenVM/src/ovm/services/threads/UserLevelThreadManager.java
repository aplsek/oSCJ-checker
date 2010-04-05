/*
 * UserLevelThreadManager.java
 *
 * Created 3 December, 2001 09:50
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/threads/UserLevelThreadManager.java,v 1.15 2004/07/19 23:24:50 dholmes Exp $
 */
package ovm.services.threads;

import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.ThreadManager;

/**
 * The <code>UserLevelThreadManager</code> provides the means for controlling 
 * and manipulating threads at the &quot;user-level&quot; by directly 
 * maintaining queues of threads. 
 * The simplest user-level thread manager simply maintains the
 * ready list, consisting of all those threads elegible to execute. 
 * <P>The order
 * in which threads are scheduled for execution is not defined.
 * <p>The exact policies used by a user-level thread manager are implementation
 * specific. For example, an implementation may maintain the currently
 * executing thread at the head of the ready list, while another implementation
 * always removes the currently executing thread from the ready list.
 * Some implementations may require that there always be one thread ready
 * to run (the &quot;idle&quot; or &quot;nop&quot; thread), while others may
 * &quot;suspend&quot; the VM until a thread is made ready through a timer
 * interrupt, or an I/O operation completion.
 *
 * <p>The methods that modify the ready queue are not expected to be 
 * thread-safe. The caller should use {@link #setReschedulingEnabled} to ensure
 * that the method executes atomically.
 * <p> A fully functional thread manager is likely to implement several 
 * different thread manager interfaces.
 *
 * @see TimePreemptiveThreadManager
 * @see TimedSuspensionThreadManager
 * @see OrderedUserLevelThreadManager
 */
public interface UserLevelThreadManager extends ThreadManager {

    /**
     * Requests that the thread manager executes the most eligible thread.
     * This is typically called soon after a change to the state of the 
     * ready queue has been made, and hence the current thread may no longer 
     * be the most eligible.
     * <p>If the current thread is no longer the most eligible for execution 
     * then it will pause until it again becomes the most elgible. If the 
     * current thread is not in the ready list then it will not run again 
     * until placed in the ready list by some other thread.
     */
    void runNextThread();

    /**
     * Inserts the given thread into the list of threads ready to execute.
     * <p>It is implementation dependent whether the thread may appear in the
     * ready list more than once. For example, a thread could appear in the
     * list multiple times to give it more execution time quanta in a time
     * preemptive thread manager. If a thread should not appear in the list
     * more than once then <code>IllegalStateException</code> may be thrown.
     *
     * <p>This is a low-level method that does not itself cause a context
     * switch to occur. The calling code is responsible for invoking
     * {@link #runNextThread} after any changes to the ready queue are made.
     *
     * <p>This method is not expected to be thread-safe.
     * @see #setReschedulingEnabled
     * @param thread the <code>OVMThread</code> to make ready
     * @throws IllegalStateException if <code>thread</code> is already in the
     * ready list and that is not permitted by this implementation.
     *
     */
    void makeReady(OVMThread thread);

    /**
     * Removes the specified thread from the ready list. 
     * <p>Whether or not it
     * is an error to attempt to remove a thread that is not in the list,
     * is implementation dependent.
     * <p>This is a low-level routine that does not itself cause a context
     * switch to occur. The calling code is responsible for invoking
     * {@link #runNextThread} after any changes to the ready queue are made.
     *
     * @param thread the <code>OVMThread</code> to remove from the ready list
     *
     * @return <code>true</code> if <code>thread</code> was found in, and
     * removed from the ready list, and <code>false</code> otherwise.
     *
     * @throws IllegalStateException if the thread is not present and this
     * implementation considers it an error to invoke this method in this
     * case
     */
    boolean removeReady(OVMThread thread);

    /**
     * Queries whether the given thread is in the ready list.
     *
     * @param thread the thread to be located
     * @return <code>true</code> if <code>thread</code> is in the ready list
     * and <code>false</code> otherwise.
     *
     */
    boolean isReady(OVMThread thread);

    /**
     * Returns the number of threads currently in the ready list
     * @return the number of threads currently in the ready list
     *
     */
    int getReadyLength();

    /**
     * Enables or disables the rescheduling of threads.
     * By disabling thread rescheduling we provide for the execution of
     * atomic sequences of code (critical regions). Services that work with
     * a user-level thread manager wil know that they can disable rescheduling
     * to allow mutually exclusive access to data structures. Exactly what is
     * involved in disabling rescheduling depends on the actual implementation.
     * For
     * example, a {@link TimePreemptiveThreadManager} would need to disable the
     * timer interrupts of the {@link ovm.core.services.timer.TimerManager}.
     * <p>Changing the enabled state is usally done as part of a paired
     * action:
     * <code><pre>
     * boolean oldState = setReschedulingEnabled(false);
     * try {
     *     // actions needing atomicity
     * }
     * finally {
     *     setReschedulingEnabled(oldState);
     * }
     * </pre></code>
     * <p>This method does not prevent {@link #runNextThread} from causing a
     * context switch, but rather prevents asynchronous context switches which
     * might occur in some implementations, for example, one using time-based
     * preemption.
     * @param enabled if <code>true</code> rescheduling of threads is enabled,
     * otherwise it is disabled.
     * @return the previous status
     * @see #isReschedulingEnabled
     *
     */
    boolean setReschedulingEnabled(boolean enabled);
    boolean setReschedulingEnabledNoYield(boolean enabled);    

    /**
     * Queries whether the rescheduling of threads is currently enabled.
     * @return <code>true</code> if the rescheduling of threads is currently
     * enabled, and <code>false</code> otherwise.
     *
     * @see #setReschedulingEnabled
     */
    boolean isReschedulingEnabled();

    /** 
     * Releases the resources associated with executing the specified thread.
     * The thread specified will typically be the current thread, but the
     * actual release of resources must be postponed until the next context
     * switch. Once that context switch has occurred the specified thread can
     * not execute again.
     * @param thread the thread to be destroyed. This thread should already 
     * have been removed from the ready queue.
     *
     */
    void destroyThread(OVMThread thread);

}











