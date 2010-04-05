/*
 * TimePreemptiveThreadManager.java
 *
 * Created 3 December, 2001 15:55
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/threads/TimePreemptiveThreadManager.java,v 1.5 2004/02/20 08:48:51 jthomas Exp $
 */
package ovm.services.threads;

import ovm.core.services.threads.ThreadManager;

/**
 * A <code>TimePreemptiveThreadManager</code> is a thread manager that
 * supports time-based preemption of threads while they are executing. This 
 * means that a thread will execute for a maximum time (the timeslice value)
 * before being forced to give up the use of a CPU.
 *
 * <p>Exactly when a thread is preempted after the timeslice expires is 
 * implementation dependent and in general will depend on the mechanism used
 * to cause time-slicing to occur.
 *
 * <p>Whether or not time-based preemption is employed at runtime will depend
 * on the Scheduler that is being used.
 *
 * <p>Whether or not the use of time-preemption can be changed at runtime
 * depends on the actual implementation.
 * Support for this can be determined
 * from the {@link #canChangePreemption} method. Even when this is
 * supported, a particular implementation may restrict when this may occur.
 * For example, an implementation may require that enabling/disabling of
 * preemption is set via a construction argument.
 *
 */
public interface TimePreemptiveThreadManager extends ThreadManager {

    /**
     * Enable time-based preemption in this thread manager. 
     * <p>Not all implementations need support this functionality. Whether
     * or not it is supported can be determined by the result of the
     * {@link #canChangePreemption} method. Even if it is
     * supported, an implementation may restrict the times when this method
     * may be called. Calling this method when it is not permitted may
     * result in an <code>IllegalStateException</code> being thrown.
     * Such restrictions should be documented by the implementing class.
     *
     * @throws IllegalStateException if the implementation does not permit preemption
     * to be enabled at this time
     * @see #disableTimePreemption
     * @see #canChangePreemption
     */
    void enableTimePreemption();

    /**
     * Disable time-based preemption in this thread manager. 
     * <p>Not all implementations need support this functionality. Whether
     * or not it is supported can be determined by the result of the
     * {@link #canChangePreemption} method. Even if it is
     * supported, an implementation may restrict the times when this method
     * may be called. Calling this method when it is not permitted may
     * result in an <code>IllegalStateException</code> being thrown.
     * Such restrictions should be documented by the implementing class.
     *
     * @throws IllegalStateException if the implementation does not permit preemption
     * to be disabled at this time
     *
     * @see #enableTimePreemption
     * @see #canChangePreemption
     *
     */
    void disableTimePreemption();

    /**
     * Queries whether time-based preemption is currently enabled
     * @return <code>true</code> if time-based preemption is enabled in this
     * thread manager, and <code>false</code> otherwise.
     *
     * @see #enableTimePreemption
     * @see #disableTimePreemption
     */
    boolean isTimePreemptionEnabled();

    /**
     * Queries whether this thread manager supports changing of the preemption
     * state.
     *
     * @return <code>true</code> if this thread manager allows time-based preemption
     * to be enabled or disabled.
     *
     * @see #enableTimePreemption
     * @see #disableTimePreemption
     *
     */
    boolean canChangePreemption();


    /**
     * Returns the minimum allowed value for the timeslice allocated to a thread
     * @return the minimum allowed value for the timeslice allocated to
     * a thread, in nanoseconds. This value will depend on the associated
     * system timers that are available.
     */
    long getMinimumTimeSlice();

    /**
     * Sets the timeslice to be used by this thread manager. 
     * <p>Not all implementations need support this functionality. Whether
     * or not it is supported can be determined by the result of the
     * {@link #canSetTimeSlice} method. Even if it is
     * supported, an implementation may restrict the times when this method
     * may be called. Calling this method when it is not permitted may
     * result in an <code>IllegalStateException</code> being thrown.
     * Such restrictions should be documented by the implementing class.
     *
     * @param timeslice the new value for the timeslice in nanoseconds
     *
     * @throws IllegalStateException if setting of the tinmeslice is not currently 
     * permitted.
     *
     * @see #getTimeSlice
     * @see #canSetTimeSlice
     *
     */
    void setTimeSlice(long timeslice);

    /**
     * Returns the current timeslice value used by this thread manager
     * @return the current timeslice value in nanoseconds
     *
     * @see #setTimeSlice
     * @see #canSetTimeSlice
     */
    long getTimeSlice();

    /**
     * Queries whether this thread manager supports the setting of the timeslice value.
     * @return <code>true</code> if this thread manager supports setting of
     * the timeslice, and <code>false</code> otherwise.
     *
     * @see #setTimeSlice
     * @see #getTimeSlice
     */
    boolean canSetTimeSlice();
}






