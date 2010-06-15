package javax.realtime;

/**
 * An {@link AsyncEvent} whose <CODE>fire</CODE> method is
 * executed periodically according to the
 * given parameters. The beginning of the first period is set or measured
 * using the clock associated with the <code>Timer</code> start time. The
 * calculation of the period uses the clock associated with the
 * <code>Timer interval</code>, unless a {@link Clock} is given,
 * in which case the calculation of the period uses that clock.
 * <BR>
 * The first firing is at the beginning of the first interval.
 * <BR>
 * If an interval greater than 0 is given, the system guarantees
 * that the timer will fire <code>interval</code> time units after the last
 * firing.
 * If an interval of 0 is given, the <code>PeriodicTimer</code> will only
 * fire once, unless restarted after expiration, behaving like for a
 * <code>OneShotTimer</code>.
 * In all cases, if the timer is <em>disabled</em> when the firing time
 * is reached, that particular firing is lost (<em>skipped</em>).
 * If <em>enabled</em> at a later time, it will fire at its next
 * scheduled time.
 * <BR>
 * If the clock time has
 * already passed the beginning of the first period,
 * the <code>PeriodicTimer</code> will fire immediately after it is
 * started.
 *
 * If one of the {@link HighResolutionTime} argument types is
 * {@link RationalTime} (now deprecated)
 * frequency times every unit time (see RationalTime constructors) by
 * adjusting the interval between executions of fire().
 * This is similar to a thread with PeriodicParameters
 * except that it is lighter weight.
 *
 * <BR>
 * Semantics details are described in the {@link Timer} pseudo-code
 * and compact graphic representation of state transitions.
 *
 * <p><b>Caution:</b> This class is explicitly unsafe in multithreaded
 * situations when it is being changed. No synchronization is done. It
 * is assumed that users of this class who are mutating instances will be
 * doing their own synchronization at a higher level.
 *
 * @spec RTSJ 1.0.1
 */
public class PeriodicTimer extends Timer{

    /** used to keep track of the clock of our interval */
    Clock intervalClock;

    /**
     * Create an instance of {@link PeriodicTimer} that executes its fire 
     * method periodically.
     *
     * @param  start     The time that specifies when the first interval
     * begins, based on the clock associated with it.
     * If <code>start</code> is <code>null</code> then the first interval will
     * start immediately upon a call to <CODE>start()</CODE>.
     * 
     * @param interval The period of the timer. Its usage is based on the
     * clock associated with it.
     * If <code>interval</code>
     * is zero, the period is ignored and the firing behavior of the
     * <code>PeriodicTimer</code> is that of a {@link OneShotTimer}.
     *
     * For a <code>PeriodicTimer</code> the method {@link #getClock()}
     * returns the <code>clock</code> to which the <code>interval</code>
     * is associated with, even when the time value of <code>interval</code>
     * is zero and the <code>PeriodicTimer</code> firing behavior is that of
     * a {@link OneShotTimer}.
     *
     * @param handler The {@link AsyncEventHandler} that will be
     * released when the timer fires.
     * If <code>null</code>, no handler is associated with this
     * <code>Timer</code> and nothing will happen when this event fires
     * unless a handler is subsequently associated with the timer using the 
     * <code>addHandler()</code> or <code>setHandler()</code> method.
     * 
     * @throws IllegalArgumentException Thrown if <code>start</code> or
     * <code>interval</code> is a <code>RelativeTime</code> instance
     * with a value less than zero.
     */
    public PeriodicTimer(HighResolutionTime start,
			 RelativeTime interval,
			 AsyncEventHandler handler){
        this(start, interval, 
             interval == null ? null : interval.clock, handler);
    }

    /**
     * Create an instance of {@link PeriodicTimer} that executes its fire
     * method periodically.
     *
     * @param  start     The time that specifies when the first interval
     * begins, based on the clock associated with it.
     * If <code>start</code> is <code>null</code> then the first interval will
     * start immediately upon a call to <CODE>start()</CODE>.
     * 
     * @param interval The period of the timer. Its usage is based on the
     * clock specified by the <code>clock</code> parameter.
     * If <code>interval</code>
     * is zero, the period is ignored and the firing behavior of the
     * <code>PeriodicTimer</code> is the one of a {@link OneShotTimer}.
     *
     * @param  clock The clock to be used to time the <code>interval</code>.
     *
     * For a <code>PeriodicTimer</code> the method {@link #getClock()}
     * returns the <code>clock</code> used to time the <code>interval</code>,
     * even when the time value of <code>interval</code>
     * is zero and the <code>PeriodicTimer</code> firing behavior is that of
     * a {@link OneShotTimer}.
     *
     * @param handler The {@link AsyncEventHandler} that will be
     * released when <code>fire</code> is invoked.
     * If <code>null</code>, no handler is associated with this
     * <code>Timer</code> and nothing will happen when this event fires
     * unless a handler is subsequently associated with the timer using the 
     * <code>addHandler()</code> or <code>setHandler()</code> method.
     * 
     * @throws IllegalArgumentException Thrown if <code>start</code> or
     * <code>interval</code> is a <code>RelativeTime</code> instance
     * with a value less than zero.
     */
    public PeriodicTimer(HighResolutionTime start,
			 RelativeTime interval,
			 Clock clock,
			 AsyncEventHandler handler){
        // we always bind with the clock of the start time. The clock
        // parameter only affects the interval
       super(start, start == null ? null : start.clock, handler);

       if (interval == null) {
           period = 0;
       }
       else if (interval.isNegative())
           throw new IllegalArgumentException("negative time");
       else {
           period = interval.toNanos();
       }
       // note we never look at interval.getClock here as our clock parameter
       // always overrides it, even if null. If you want to use interval's
       // clock then don't use this form of the constructor.
       intervalClock = (clock != null ? clock : Clock.rtc);
       periodic = true;
       gotoState(currentState);
    }


    /**
     * Return the instance of {@link Clock} associated with the interval
     * of this <code>PeriodicTimer</code>.
     *
     * @return The instance of {@link Clock} associated with the interval of
     * this <code>PeriodicTimer</code>.
     * 
     * 
     * @throws IllegalStateException {@inheritDoc}
     */
    public Clock getClock(){
        synchronized(lock) {
            checkDestroyed();
            return intervalClock;
        }
    }

    
    /**
     * Create a {@link PeriodicParameters} object with a start time and period
     * that correspond to the next firing (or skipping) time, and interval,
     * of this timer.
     * <p>
     * If this timer is active, then the start time is the next firing (or
     * skipping) time as an {@link AbsoluteTime}. 
     * Otherwise, the start time is the initial firing (or skipping) time,
     * as set by the last call to {@link Timer#reschedule reschedule}, or if
     * there was no such call, by the constructor of this timer.
     *
     * @return  A new instance of {@link PeriodicParameters}
     * with values corresponding to the release characteristics of this timer.
     *
     * @throws IllegalStateException {@inheritDoc}
     *
     * @specbug It is not specified what clock is associated with the start 
     * time object in the created release parameters.
     */
    public ReleaseParameters createReleaseParameters(){
        synchronized(lock) {
            checkDestroyed();

            RelativeTime p = new RelativeTime(0,0, intervalClock);
            p.setDirect(period);
            
            switch(currentState) {
            case NA_D_A: {
                AbsoluteTime s = new AbsoluteTime(0,0, clock);
                s.setDirect(nextTargetTime);
                return new PeriodicParameters(s, p);
            }
            case NA_D_R: {
                RelativeTime s = new RelativeTime(0,0, clock);
                s.setDirect(nextDurationTime);
                return new PeriodicParameters(s, p);
            }
            case A_D_A:
            case A_E_A:
            case A_D_R:
            case A_E_R:
                return new PeriodicParameters(getFireTime(), p);
            default:
                throw new InternalError("Unexpected state" + currentState);
            }
        }
    }

    /**
     * @specbug This override isn't specified, but we need to change the
     * clock associated with the fireTime if we have fired or skipped.
     */
    public AbsoluteTime getFireTime() {
        AbsoluteTime t = super.getFireTime();
        if (firedOrSkipped)
            t.clock = intervalClock;
        return t;
    }

    /**
     * Gets the interval of <code>this</code> <code>Timer</code>.
     *
     * @return A new <code>RelativeTime</code> instance
     * with the value of this timer's <code>interval</code>.
     *
     * @throws IllegalStateException Thrown if this <code>Timer</code>
     * has been <em>destroyed</em>.
     */
    public RelativeTime getInterval(){
        synchronized(lock) {
            checkDestroyed();
            RelativeTime t = new RelativeTime(0,0, intervalClock);
            t.setDirect(period);
            return t;
        }
    }

    /**
     * Reset the <code>interval</code> value of <code>this</code>.
     *
     * @param interval A {@link RelativeTime} object which is the 
     * interval used to reset this Timer.
     * The semantics of the update are the same as for
     * updating the period of a real-time thread. (See the semantics of
     * the priority scheduler.)
     *
     * @throws IllegalArgumentException Thrown if <code>interval</code>
     * is a <code>RelativeTime</code> instance with a value less than zero.
     *
     * @throws IllegalStateException Thrown if this <code>Timer</code>
     * has been <em>destroyed</em>.
     */
    public void setInterval(RelativeTime interval){
        synchronized(lock) {
            checkDestroyed();
            if (interval == null) {
                period = 0;
                intervalClock = Clock.rtc;
            }
            else if (interval.isNegative())
                throw new IllegalArgumentException("negative time");
            else {
                period = interval.toNanos();
                intervalClock = interval.clock;
            }
        }
    }



    /**
     * Set our next firing time by adding the current period, and inform
     * the timer thread of the change
     */
    void selfReschedule() {
        // this is only called when period > 0

        // when this is called we have already been removed from the queue
        // so we just need to add ourselves back to it

        if (currentState == A_E_A || currentState == A_D_A) {
            // will write to targetTime
            absThread.add(this, targetTime+period);
        }
        else { // relative
            countingTime = 0;
            // next referenceTime = last referenceTime + last durationTime
            // will write to referenceTime and durationTime
            relThread.add(this, referenceTime+durationTime, period);
        }
    }

}




