package javax.realtime;

/**
 * This release parameter indicates that the 
 * {@link RealtimeThread#waitForNextPeriod} method on the associated 
 * {@link Schedulable} object will be unblocked at the start of each period.
 * When a reference to a <code>PeriodicParameters</code> object is given
 * as a parameter to a constructor the <code>PeriodicParameters</code> 
 * object becomes bound  to the object being created. 
 * Changes to the values in the <code>PeriodicParameters</code> object affect 
 * the constructed object. 
 * If given to more than one constructor then changes to the values in the 
 * <code>PeriodicParameters</code> object affect <em>all</em> of the associated
 *  objects. Note that this is a one-to-many relationship and <em>not</em> a 
 * many-to-many.
 *
 * <p><b>Caution:</b> This class is explicitly unsafe in multithreaded
 * situations when it is being changed.  No synchronization is done.  It
 * is assumed that users of this class who are mutating instances will be
 * doing their own synchronization at a higher level.
 *
 * <h3>OVM Notes</h3>
 * <p>I have streamlined and made consistent the descriptions given here.
 * 
 * <p><b>Notes on usage:</b> Periodic parameters only make sense for threads.
 * Sporadic parameters generally only make sense for AsyncEventHandlers
 * (though a thread could manually construct a "trigger" mechanism similar to
 * waitForNextPeriod).
 * <p>For consistency with the similar methods of Schedulable, the
 * setIfFeasible methods leave a value unchanged if null is passed.
 *
 * <h3>To-Do</h3>
* <p>Support RationalTime for the period. 
 * @author David Holmes
 */
public class PeriodicParameters extends ReleaseParameters {

   /** the start time */
    protected HighResolutionTime start;

    /** the period */
    protected final RelativeTime period;


    public String toString() {
        return "Start = "+start+", Period = "+period + ", Cost = " + cost;
    }

    // need to add new docs here
    public PeriodicParameters(HighResolutionTime start, RelativeTime period) {
        this(start, period, null, null, null, null);
    }

    /** 
     * Create a <code>PeriodicParameters</code> object.
     *
     * @param start Time at which the first period begins. If a
     * {@link RelativeTime} , this time is relative to the first time the
     * {@link Schedulable} object becomes schedulable (schedulable time)
     * (e.g., when start() is called on a thread). If an
     * {@link AbsoluteTime} and it is before the schedulable time, start is
     * equivalent to the schedulable time.
     *
     * @param period The period is the interval between successive unblocks 
     * of {@link RealtimeThread#waitForNextPeriod} . 
     * Must be greater than zero when entering feasibility analysis.
     *
     * @param cost Processing time per period. On implementations which can
     * measure the amount of time a schedulable object is executed, this value
     * is the maximum amount of time a schedulable object receives per period.
     * On implementations which cannot measure execution time, this value is 
     * used as a hint to the feasibility algorithm. On such systems it is not 
     * possible to determine when any particular object exceeds or will exceed
     * <code>cost</code> time units in a period. 
     * Equivalent to <code>RelativeTime(0,0)</code> if <code>null</code>.
     *
     * @param deadline The latest permissible completion time measured from
     * the release time of the associated invocation of the schedulable
     * object. For a minimum implementation for purposes of feasibility 
     * analysis, the deadline is equal to the period. Other implementations 
     * may use this parameter to compute execution eligibility. 
     * If <code>null</code>, deadline will equal the period.
     *
     * @param overrunHandler This handler is invoked if an invocation of the
     * schedulable object exceeds cost. 
     * Not required for minimum implementation. If <code>null</code>, nothing 
     * happens on the overrun condition, and 
     * {@link RealtimeThread#waitForNextPeriod waitForNextPeriod} returns 
     * <code>false</code> immediately and updates the start time for the next 
     * period.
     *
     * @param missHandler This handler is invoked if an invocation of the 
     * {@link Schedulable} object is still executing after the 
     * deadline has passed. Although minimum implementations do not consider
     * deadlines in feasibility calculations, they must recognize
     * variable deadlines and invoke the miss handler as appropriate. If
     * <code>null</code>, nothing happens on the miss deadline condition.
     */
    public PeriodicParameters(HighResolutionTime start,
			      RelativeTime period,
			      RelativeTime cost,
			      RelativeTime deadline,
			      AsyncEventHandler overrunHandler,
			      AsyncEventHandler missHandler )
    {
        super(cost, deadline, overrunHandler, missHandler);
	if (start == null) {
            // set to the only time guaranteed to have passed
	    this.start = new AbsoluteTime(0,0);
        }
	else {
	    this.start = start;
        }
        if (period == null || period.isNegative()) {
            throw new IllegalArgumentException("Need a period > 0");
        }
        else if (period instanceof RationalTime) {
            throw new UnsupportedOperationException(
                "Don't have rational time support");
        } 
        else {
            this.period = new RelativeTime(period);
        }
	if (deadline == null) {
	    this.deadline.set(period);
        }
    }

    /**
     * Get the value of period.
     * @return An instance of {@link RelativeTime} that represents the value 
     * of period.
     */
    public RelativeTime getPeriod() {
	return new RelativeTime(period);
    }

    // avoid defensive copy
    long getPeriodNanos() {
        return period.toNanos();
    }

    /**
     * Get the value of start.
     * @return An instance of {@link HighResolutionTime} representing the value
     * of start.
      */
    public HighResolutionTime getStart() {
	return start; 
    }

    /**
     * @param deadline The latest permissible completion time measured from
     * the release time of the associated invocation of the schedulable
     * object. For a minimum implementation for purposes of feasibility 
     * analysis, the deadline is equal to the period. Other implementations 
     * may use this parameter to compute execution eligibility. 
     * If <code>null</code>, deadline will equal the period.
     */
    public void setDeadline(RelativeTime deadline) {
        super.setDeadline( deadline != null ? deadline : period);
    } 

    /**
     * Sets the value of the period
     * @param period The period is the interval between successive unblocks 
     * of {@link RealtimeThread#waitForNextPeriod} . 
     * Must be greater than zero when entering feasibility analysis.
     */
    public void setPeriod(RelativeTime period) {
        if (period == null || period.isNegative()) {
            throw new IllegalArgumentException("Need a period > 0");
        }
        if (period instanceof RationalTime) {
            throw new UnsupportedOperationException(
                "Don't have rational time support");

        }
	this.period.set(period);
    }
    
    /**
     * Sets the value of start
     * @param start Time at which the first period begins. If a
     * {@link RelativeTime} , this time is relative to the first time the
     * {@link Schedulable} object becomes schedulable (schedulable time)
     * (e.g., when start() is called on a thread). If an
     * {@link AbsoluteTime} and it is before the schedulable time, start is
     * equivalent to the schedulable time.
     */
    public void setStart(HighResolutionTime start) {
	if (start == null) {
            // set to the only time guaranteed to have passed
	    this.start = new AbsoluteTime(0,0);
        }
	else {
	    this.start = start;
        }
    }


    /**
     * Replaces the existing scheduling values with the supplied ones, if the
     * resulting characteristics yield a feasible system.
     * <p>This method first performs a feasibility analysis using the new 
     * scheduling values as replacements for the current scheduling values.
     * If the resulting system is feasible the method replaces the current 
     * scheduling values with the new scheduling values.
     * <p><b>As there is no support for a feasibility test this method always
     * returns <code>true</code></b>
     *
     * @param period the proposed period. If <code>null</code> the value
     * is left unchanged.
     * @param cost the proposed cost. If <code>null</code> the value
     * is left unchanged.
     * @param deadline the proposed deadline. If <code>null</code> the value
     * is left unchanged.
     *
     * @return <code>true</code> if the resulting system is feasible and the 
     * changes are made.
     * <code>false</code>, if the resulting system is not feasible and no 
     * changes are made.
     */
    public boolean setIfFeasible(RelativeTime period, 
                                 RelativeTime cost, 
                                 RelativeTime deadline) {
        if (period != null) {
            setPeriod(period);
        }
        if (cost != null) {
            setCost(cost);
        }
        if (deadline != null) {
            setDeadline(deadline);
        }
	return true;
    }


}











