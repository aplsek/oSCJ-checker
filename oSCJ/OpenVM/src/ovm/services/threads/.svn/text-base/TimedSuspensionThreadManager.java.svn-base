/*
 * TimedSuspensionThreadManager.java
 *
 * Created 4 December, 2001 15:55
 */
package ovm.services.threads;

import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.ThreadManager;
/**
 * The <code>TimedSuspensionThreadManager</code> supports suspending a thread 
 * for at least a given relative time, or until an absolute time has passed. 
 *
 * <p>The logical service provided by this thread manager is one of a 
 * &quot;sleep queue&quot; to which threads can be added or removed. 
 * Whether this is the way it is actually implemented
 * depends on the implementation. When used in conjunction with a user-level 
 * thread manager it is likely that a queue will be used; while a native 
 * thread manager will probably (but not necessarily) use a native 
 * sleep mechanism.
 *
 * <p>The primary use of this service to provide threads with the ability to 
 * sleep and perform timed-waits (as, for example, in the case of a timed 
 * condition variable wait).
 *
 * <p>Depending on the implementation it may be possible to wakeup a thread 
 * prior to the expiration of its sleep time. Whether or not wakeup is 
 * supported can be determined by invoking the {@link #canWakeUp } method.
 *
 * <p>It is common practice for only the current thread to be able to suspend 
 * itself, but this is not an enforced requirement of this service. 
 * A particular implementation may refine the specification of 
 * {@link #sleep sleep} to enforce this restriction. 
 *
 * <p>Note that as this service does not necessarily cause the current thread 
 * to cease execution during the call to <code>sleep</code>, there is no way 
 * to report to the thread whether its sleep time elapsed or it was woken up. 
 * That level of functionality
 * must be provided at a higher-level, for example, by having the caller of 
 * {@link #wakeUp wakeUp} set a flag that the thread can query
 *
 */
public interface TimedSuspensionThreadManager extends ThreadManager {

    /**
     * Adds the specified thread to the sleep queue for at least the given
     * number of nanoseconds. The thread remains in 
     * the sleep queue until at least the specified number of nanoseconds 
     * elapses or the thread is removed via a call to {@link #wakeUp wakeUp} 
     * (if supported). This is a relative sleep that should not be affected
     * by changes to the system clock.
     * <p>The thread may remain in the queue longer than the specified time 
     * depending on the timer resolution of the implementation, but will never
     * be removed from the sleep queue at an earlier time. 
     * To enforce this an implementation may need to pad the sleep time
     * to avoid jitter caused by the granularity of the timer interrupt. 
     * It should be remembered that sleep mechanisms are inherently 
     * coarse-grained and are only effective when the sleep times required 
     * are at least an order of magnitude greater than the resolution of the 
     * timer. An implementation may simply add the value returned by
     * {@link #getSleepResolution} to any specified sleep time to ensure the 
     * minimum sleep requirement is met.
     * <p>The actual time that a thread is not eligible for execution during 
     * a sleep depends on the scheduler being used and the system load.
     * <p>If the thread to be put to sleep is the current thread then this 
     * method will not return until the sleep time has elapsed 
     * (or the thread was woken up) and the thread has again been selected to 
     * execute.
     *
     * <p>It is implementation dependent whether the thread may appear in the
     * sleep queue more than once.  If a thread should not appear in the queue
     * more than once then <code>OVMError.IllegalState</code> may be thrown.
     *
     * @param thread the thread to place in the sleep queue
     * @param nanos the minimum time, in nanoseconds, that the thread 
     * should remain in the sleep queue
     * @throws OVMError.IllegalArgument if <tt>sleepTime</tt> is <= 0.
     * @throws OVMError.IllegalState if <code>thread</code> is already in the
     * sleep queue and that is not permitted by this implementation.
     * @see #sleepAbsolute
     * @see #wakeUp
     * @see #getSleepResolution
     */
    void sleep(OVMThread thread, long nanos);

    /**
     * Adds the specified thread to the sleep queue if the absolute wake-up
     * time has not passed. The thread will remain in the sleep queue until
     * the wake-up time arrives, or the thread is woken up (if supported).
     * Absolute sleeps are affected by changes to the system clock.
     * <p>The thread may remain in the queue longer than the specified time 
     * depending on the timer resolution of the implementation, but will never
     * be removed from the sleep queue at an earlier time. 
     * <p>The actual time that a thread is not eligible for execution during 
     * a sleep depends in the scheduler being used and the system load.
     * p>If the thread to be put to sleep is the current thread then this 
     * method will not return until the sleep time has elapsed 
     * (or the thread was woken up) and the thread has again been selected to 
     * execute.
     *
     * <p>It is implementation dependent whether the thread may appear in the
     * sleep queue more than once.  If a thread should not appear in the queue
     * more than once then <code>IllegalStateException</code> may be thrown.
     *
     * @param thread the thread to place in the sleep queue
     * @param wakeupTime the absolute time, in nanoseconds since the epoch,
     * at which the thread should be removed from the sleep queue
     * @return <code>true</code> if the thread was added to the sleep queue,
     * and <code>false</code> if the wake-up time had already passed and so
     * the thread was not added.
     * @throws OVMError.IllegalState if <code>thread</code> is already in the
     * sleep queue and that is not permitted by this implementation.
     * @see #sleep
     * @see #wakeUp
     * @see #getSleepResolution
     */
    boolean sleepAbsolute(OVMThread thread, long wakeupTime);

    /**
     * Removes the specified thread from the sleep queue and makes it ready
     * to run.
     * <p>Whether or not it
     * is an error to attempt to remove a thread that is not in the queue
     * is implementation dependent.
     *
     * @param thread the thread to remove from the sleep queue
     *
     * @return <code>true</code> if <code>thread</code> was found in, and
     * removed from the sleep queue, and <code>false</code> otherwise.
     *
     * @throws OVMError.IllegalState if the thread is not present and this
     * implementation considers it an error to invoke this method in this
     * case; or <code>wakeUp</code> is not supported.
     *
     * @see #sleep
     * @see #canWakeUp
     */
    boolean wakeUp(OVMThread thread);

    /**
     * Queries whether a given thread is in the sleep queue
     *
     * @param thread the thread to locate in the sleep queue
     * @return <code>true</code> if <code>thread</code> is in the sleep queue
     * and <code>false</code> otherwise.
     *
     */
    boolean isSleeping(OVMThread thread);


    /**
     * Returns the smallest time interval used to update the sleep queue. 
     * This will usually be some multiple of the timer interrupt period being
     * used by the {@link ovm.core.services.timer.TimerManager TimerManager}.
     *
     * @return the smallest time interval, in nanoseconds, used to update the 
     * sleep queue 
     *
     */
    long getSleepResolution();

    /**
     * Queries whether this thread manager supports waking up of threads in 
     * the sleep queue
     *
     * @return <code>true</code> if {@link #wakeUp} calls are supported and 
     * <code>false</code> otherwise
     *
     */
    boolean canWakeUp();
}




