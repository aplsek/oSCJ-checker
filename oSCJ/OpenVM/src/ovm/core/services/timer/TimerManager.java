/*
 * TimerManager.java
 *
 * Created 30 November, 2001 11:25
 *
 */
package ovm.core.services.timer;


/**
 * <p>The <code>TimerManager</code> provides two mechanism for interacting with
 * time related actions:
 * <ul>
 * <li>A low-level interface to the timer interrupt mechanism provided by 
 * operating systems. 
 * <p>It maintains a list of {@link TimerInterruptAction} objects, for which 
 * it invokes the <code>fire</code> method upon each timer interrupt. 
 * <li>A pair of delay queues (one for relative delays and one for absolute)
 * to which instances of {@link DelayableObject} can be added.
 * When the requested delay has expired, the object is removed from the
 * queue, and a custom action performed on the object.
 * <p>This is intended for things like sleep queues, and other forms of timed
 * waits, such as those used with monitors.
 * </ul>
 * <p>On most systems it is expected that the
 * <code>TimerManager</code> will be a singleton class.
 *
 * <p>Some systems may support the setting of the timer interrupt period.
 * Whether or not this is supported can be determined
 * from the {@link #canSetTimerInterruptPeriod} method. Even when this is
 * supported, a particular implementation may restrict when this may occur.
 * For example, it may only be set prior to the timer manager being started.
 *
 * <p>The <tt>TimerManager</tt> interoperates with the <tt>EventManager</tt>
 * by registering itself as an event processor - in this case for timer events.
 *
 * <h3>Timer Interrupt Actions</h3>
 * <p>Each action is fired on every interrupt. If this results in too fast a
 * firing of a particular action, then it is up to the action to decide
 * how to respond to the firing. An action can query the timer interrupt
 * period to work out how it should respond. If the actions take longer
 * to execute than the interrupt period, the results are implementation
 * dependent.
 *
 * <p>The firing of an action is generally an asynchronous event with respect
 * to the normal execution flow of a thread. The behaviour is as-if the action
 * is executed in a separate thread which can execute at any time with the
 * highest priority in the system. As such a <code>fire</code>
 * method should only perform actions that are known to be async safe, or
 * conversely steps must be taken to ensure an action will not fire if its
 * execution would be unsafe. The simplest action sets a flag, or value,
 * that can then be checked synchronously.
 * <p>The need for async-safety conflicts with the basic intent of the timer
 * manager: supporting the implementation of sleep queues and time preemptive
 * scheduling. In short, a fired action <b>must not</b> cause a context switch
 * to occur as this would prevent the rest of the actions from firing. 
 *
 * <p><b>No language-level synchronization should be used within a 
 * <code>fire</code> method.</b>
 *
 * <h3>Delay Queue Operation</h3>
 * <p>The delay queues are processed as if by a timer interrupt action defined
 * by the timer manager itself. Consequently, all the restrictions of fire
 * methods applies to the custom action methods of {@link DelayableObject}.
 * <p>It is dependent on the implementation whether the delay queue methods
 * ensure atomicity, or whether the caller must guarantee atomicity themselves.
 *
 * @see TimerInterruptAction
 * @see ovm.core.services.events.EventManager
 */
public interface TimerManager extends ovm.services.ServiceInstance {

    /**
     * A <tt>DelayableObject</tt> can be inserted into a delay queue.
     */
    public interface DelayableObject {

        /**
         * Invoked when this <tt>DelayableObject</tt> is removed from a
         * delay queue due to the expiration of its delay.
         */
        void delayExpired();
    }


    /**
     * Adds the given <tt>DelayableObject</tt> to the relative delay queue.
     * @param obj the object to delay
     * @param delay the delay time
     * @throws OVMError.IllegalArgument If <tt>delay</tt>  is <= 0
     */
    void delayRelative(DelayableObject obj, long delay);

    /**
     * Adds the given <tt>DelayableObject</tt> to the absolute delay queue.
     * @param obj the object to delay
     * @param delay the delay time
     * @return <tt>true</tt> if <tt>delay</tt> has not 
     * passed and so <tt>obj</tt> was delayed; otherwise if <tt>delay</tt> 
     * has already passed return <tt>false</tt>.
     */
    boolean delayAbsolute(DelayableObject obj, long delay);

    /**
     * Removes the given object from the relative delay queue.
     * @param obj the object to remove
     * @return <tt>true</tt> if <tt>obj</tt> was found and removed, and
     * <tt>false</tt> otherwise.
     */
    boolean wakeUpRelative(DelayableObject obj);

    /**
     * Removes the given object from the absolute delay queue.
     * @param obj the object to remove
     * @return <tt>true</tt> if <tt>obj</tt> was found and removed, and
     * <tt>false</tt> otherwise.
     */
    boolean wakeUpAbsolute(DelayableObject obj);

    /**
     * Returns <tt>true</tt> if the given object is in the relative delay
     * queue.
     * @param obj the object to query
     */
    boolean isDelayedRelative(DelayableObject obj);

    /**
     * Returns <tt>true</tt> if the given object is in the absolute delay
     * queue.
     * @param obj the object to query
     */
    boolean isDelayedAbsolute(DelayableObject obj);


    /**
     * Adds the specified {@link TimerInterruptAction} to the list of
     * actions to be fired when the timer interrupt occurs. 
     *
     * @param tia the <code>TimerInterruptAction</code> to add
     * @see TimerInterruptAction
     * @see #removeTimerInterruptAction
     */
    void addTimerInterruptAction(TimerInterruptAction tia);

    /**
     * Removes the specified <code>TimerInterruptAction</code> from the list of
     * actions to be fired when the timer interrupt occurs. 
     *
     * @param tia the <code>TimerInterruptAction</code> to remove
     * @see TimerInterruptAction
     * @see #addTimerInterruptAction
     */
    void removeTimerInterruptAction(TimerInterruptAction tia);

    /** 
     * Return an array of all the <code>TimerInterruptAction</code> objects
     * registered to fire when a timer interrupt occurs.
     *
     * @return an array containing all registered <TimerInterruptAction</code>
     * objects. If no objects are registered a zero length array is returned.
     *
     * @see #addTimerInterruptAction
     * @see #removeTimerInterruptAction
     * @see TimerInterruptAction
     *
     */
    TimerInterruptAction[] getRegisteredActions();

    /**
     * Return the period of the timer interrupt in nanoseconds
     *
     * @return the period of the timer interrupt in nanoseconds. 
     *
     * @see #setTimerInterruptPeriod
     */
    long getTimerInterruptPeriod();


    /**
     * Requests that the timer interrupt period be set to the given value.
     * Not all implementations need support this functionality. Whether
     * or not it is supported can be determined by the result of the
     * {@link #canSetTimerInterruptPeriod} method. Even if it is
     * supported, an implementation may restrict the times when this method
     * may be called. Calling this method when it is not permitted may
     * result in an <code>IllegalStateException</code> being thrown.
     * Such restrictions should be documented by the implementing class.
     * It also may not be possible to set the period to the desired value, 
     * in which case the method will return <code>false</code>.
     * 
     *
     * @param period the new timer interrupt period in nanoseconds
     * @return <code>true</code> if the period was changed and 
     * <code>false</code> otherwise.
     * @throws IllegalStateException if the implemententation does not
     * support setting of the period.
     * @see #getTimerInterruptPeriod
     *
     */
    boolean setTimerInterruptPeriod(long period);
    
    boolean setTimerInterruptPeriodMultiplier(long multiplier);

    /**
     * Queries if this <code>TimerManager</code> supports the setting of
     * the interrupt period.
     *
     * @return <code>true</code> if setting of the timer interrupt period
     * is supported, and <code>false</code> otherwise.
     *
     * @see #setTimerInterruptPeriod
     */
    boolean canSetTimerInterruptPeriod();

    /**
     * Start the operation of the timer manager. Generally, this will cause
     * the timer manager to install all necessary system hooks so that
     * timer events will be processed, and configure the interrupt
     * period to that requested (if supported).
     * <p>It is an error to try to start a timer manager that has been started.
     * However, an implementation may allow you to re-start a timer manager
     * after it has been {@link #stop stopped}. Whether or not the timer
     * manager can be restarted can be determined by the {@link #canRestart}
     * method.
     *
     * @see #stop
     * @see #canRestart
     * @see #isRunning
     * @throws IllegalStateException if the timer manager has already been
     * started and {@link #stop} has not been invoked.
     */
    void start();

    /**
     * Stops the operation of the timer manager. Once stopped, no further
     * actions will be fired until the timer manager is restarted (if 
     * supported). Generally, this method should remove all of the system
     * hooks put in place by {@link #start}.
     * <p>Stopping a timer manager that has not been started has no affect.
     *
     * @see #start
     * @see #canRestart
     * @see #isRunning
     *
     */
    void stop();

    /**
     * Queries whether the timer manager is currently running.
     *
     * @return <code>true</code> if {@link #start} has been invoked and 
     * {@link #stop} has not subsequently been invoked, and <code>false</code>
     * otherwise.
     *
     * @see #start
     * @see #stop
     */
    boolean isRunning();

    /**
     * Queries whether this timer manager can have {@link #start} invoked again
     * after {@link #stop} has been invoked.
     *
     * @return <code>true</code> if {@link #start} can be invoked again and
     * <code>false</code> otherwise.
     *
     * @see #start
     * @see #stop
     */
    boolean canRestart();
}





















































































