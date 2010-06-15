package javax.realtime;

/**
 * A timed {@link AsyncEvent} that is driven by a {@link Clock}.
 * It will fire off once,
 * when the clock time reaches the time-out time, unless restarted after
 * expiration.
 * If the timer is <em>disabled</em> at the expiration of the indicated time,
 * the firing is lost (<em>skipped</em>).
 * After expiration, the <code>OneShotTimer</code> becomes
 * <em>not-active</em> and retains the <em>enabled</em> or <em>disabled</em>
 * state it had at expiration time.
 * If the clock time has
 * already passed the time-out time, it will fire immediately after it is
 * started or after it is rescheduled while <em>active</em>.
 *
 * <BR>
 * Semantics details are described in the {@link Timer} pseudocode
 * and compact graphic representation of state transitions.
 *
 * <p><b>Caution:</b> This class is explicitly unsafe in multithreaded
 * situations when it is being changed. No synchronization is done. It
 * is assumed that users of this class who are mutating instances will be
 * doing their own synchronization at a higher level.
 *
 * @spec RTSJ 1.0.1
 */
public class OneShotTimer extends Timer{

    /** 
     * Create an instance of {@link OneShotTimer}, based on the {@link Clock}
     * associated with the <code>time</code> parameter, that will execute its
     * <code>fire</code> method according to the given time.
     *
     * @param time The time at which the handler is released.
     * A null value of <code>time</code> is equivalent to a
     * relative time of 0.
     *
     * @param handler The {@link AsyncEventHandler} that will be
     * released when <code>fire</code> is invoked.
     * If <code>null</code>, no handler is associated with this
     * <code>Timer</code> and nothing will happen when this event fires
     * unless a handler is subsequently associated with the timer using the 
     * <code>addHandler()</code> or <code>setHandler()</code> method.
     * 
     * @throws IllegalArgumentException Thrown if <code>time</code>
     * is a <code>RelativeTime</code> instance less than zero.
     * 
     */
    public OneShotTimer(HighResolutionTime time ,AsyncEventHandler handler){
       this(time, time == null ? null : time.getClock(), handler);
    }

    /**
     * Create an instance of {@link OneShotTimer}, based on the given
     * <code>clock</code>, that will execute its
     * <code>fire</code> method according to the given time.
     * The {@link Clock} association of the parameter <code>time</code>
     * is ignored.
     *
     * @param time The time at which the handler is released.
     * A null value of <code>time</code> is equivalent to a
     * relative time of 0.
     *
     * @param clock The timer will be based on this <code>clock</code>.
     * If the <code>clock</code> is <code>null</code>, the default
     * <code>Realtime clock</code> is used.
     *
     * @param handler The {@link AsyncEventHandler} that will be
     * released when <code>fire</code> is invoked.
     * If <code>null</code>, no handler is associated with this
     * <code>Timer</code> and nothing will happen when this event fires
     * unless a handler is subsequently associated with the timer using the 
     * <code>addHandler()</code> or <code>setHandler()</code> method.
     * 
     * @throws IllegalArgumentException Thrown if <code>time</code>
     * is a <code>RelativeTime</code> instance less than zero.
     *
     */
    public OneShotTimer(HighResolutionTime time, Clock clock, 
                        AsyncEventHandler handler){
       super(time, clock, handler);
       periodic = false;
       gotoState(currentState);
    }
}


