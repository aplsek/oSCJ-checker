/*
 * TimerManager.java
 *
 * Created 30 November, 2001 11:25
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/core/services/timer/TimerInterruptAction.java,v 1.5 2004/10/13 02:51:06 pizlofj Exp $
 */
package ovm.core.services.timer;


/**
 * A <code>TimerInterruptAction</code> can be registered with the
 * <code>TimerManager</code> so that its <code>fire</code> method is
 * invoked on every timer interrupt.
 *
 * @see TimerManager
 */
public interface TimerInterruptAction {

    /**
     * This method is invoked every time a timer interrupt occurs for
     * the <code>TimerManager</code> with which this action is
     * registered, and the timer manager has interrupts enabled.
     * Because interrupts can be disabled, the timer manager passes the
     * number of interrupts that should have occurred since this action
     * was last fired.
     * <p>The firing of an action is generally an asynchronous event with respect
     * to the normal execution flow of a thread. The behaviour is as-if the action
     * is executed in a separate thread which can execute at any time with the
     * highest priority in the system. As such a <code>fire</code>
     * method should only perform actions that are known to be async safe, or
     * conversely steps must be taken to ensure an action will not fire if its
     * execution would be unsafe. The simplest action sets a flag, or value,
     * that can then be checked synchronously.
     * <p><b>No language-level synchronization should be used within a 
     * <code>fire</code> method.</b>
     *
     * @param ticks the number of interrupts that have occurred since this method
     * was last invoked
     * @see TimerManager#addTimerInterruptAction
     * @see TimerManager#removeTimerInterruptAction
     * @see TimerManager
     */
    void fire(int ticks);

    String timerInterruptActionShortName();
}
