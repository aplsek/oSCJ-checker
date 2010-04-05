package javax.realtime;
/** 
 * The abstract top-level class for release characteristics of threads.
 * When a reference to a <code>ReleaseParameters</code> object is given
 * as a parameter to a constructor, the <code>ReleaseParameters</code> 
 * object becomes bound to the object being created. Changes to the values 
 * in the <code>ReleaseParameters</code> object affect the constructed object.
 * If given to more than one constructor, then changes to the values in the 
 * <code>ReleaseParameters</code> object affect <em>all</em> of the associated
 * objects. Note that this is a one-to-many relationship and <em>not</em> a 
 * many-to-many.
 *
 * <p><b>Caution:</b> This class is explicitly unsafe in multithreaded
 * situations when it is being changed.  No synchronization is done.  It
 * is assumed that users of this class who are mutating instances will be
 * doing their own synchronization at a higher level.
 *
 * <p><b>Caution:</b> The <code>cost</code> parameter time should be 
 * considered to be measured against the target platform.
 *
 * <h3>OVM Notes</h3>
 * <p>Many of the doc comments for this class give details that belong in
 * subclasses; for example, talking about periods or inter-arrival times.
 * I have stripped the docs back to the bare minimum suitable for this base
 * class. I have also used consistent terminology when describing handlers.
 *
 * <p>For consistency with the similar methods of Schedulable, the
 * setIfFeasible methods leave a value unchanged if null is passed.
 *
 * @author David Holmes
 */
public class ReleaseParameters extends ParameterBase {
    
    /** the execution cost eg worst-case-execution-time */
    protected final RelativeTime cost = new RelativeTime();

    /** the deadline */
    protected final RelativeTime deadline = new RelativeTime();

    /** Handler run when cost is exceeded */
    protected AsyncEventHandler overrunHandler;

    /** Handler run when deadline is missed */
    protected AsyncEventHandler missHandler;

    /** Create an instance of <code>ReleaseParameters</code> */
    protected ReleaseParameters() { }
    
    /**
     * Create a new instance of ReleaseParameters with the given parameter 
     * values.
     *
     * @param cost Processing time units per interval. On implementations which
     * can measure the amount of time a schedulable object is executed, this 
     * value is the maximum amount of time a schedulable object receives per 
     * interval. On implementations which cannot measure execution time, this 
     * value is used as a hint to the feasibility algorithm. On such systems 
     * it is not possible to determine when any particular object exceeds cost.
     * Equivalent to <code>RelativeTime(0,0)</code> if <code>null</code>.
     *
     * @param deadline The latest permissible completion time measured from
     * the release time of the associated invocation of the schedulable
     * object. Changing the deadline might not take effect after the
     * expiration of the current deadline. More detail provided in the
     * subclasses.
     *
     * @param overrunHandler This handler is invoked if an invocation of the
     * {@link Schedulable} object exceeds cost. 
     * Not required for minimum implementation. If <code>null</code>, nothing 
     * happens on the overrun condition.
     *
     * @param missHandler This handler is invoked if an invocation of the 
     * {@link Schedulable} object is still executing after the 
     * deadline has passed. Although minimum implementations do not consider
     * deadlines in feasibility calculations, they must recognize
     * variable deadlines and invoke the miss handler as appropriate. If
     * <code>null</code>, nothing happens on the miss deadline condition.
     *
     */
    protected ReleaseParameters(RelativeTime cost,
				RelativeTime deadline,
				AsyncEventHandler overrunHandler,
				AsyncEventHandler missHandler)
    {
        if (cost != null) {
            if (cost.isNegative()) {
                throw new IllegalArgumentException("can't have negative cost");
            }
            this.cost.set(cost);
        }
        if (deadline != null) {
            if (deadline.isNegative()) {
                throw new IllegalArgumentException("can't have negative deadline");
            }
            this.deadline.set(deadline);
        }
	this.overrunHandler = overrunHandler;
	this.missHandler = missHandler;
    }


    /**
     * Gets the value of cost.
     * @return the value of cost
     */
    public RelativeTime getCost() {
	return new RelativeTime(cost);
    }
    
    /**
     * Gets the cost overrun handler.
     * @return A reference to an instance of {@link AsyncEventHandler} that is
     * cost overrun handler for this.
     */
    public AsyncEventHandler getCostOverrunHandler() {
	return overrunHandler;
    }
    
    /**
     * Gets the value of deadline.
     * @return A reference to an instance of {@link RelativeTime} that is the 
     * deadline of this.
     */
    public RelativeTime getDeadline() {
	return new RelativeTime(deadline);
    }

    // avoid defensive copy
    long getDeadlineNanos() {
        return deadline.toNanos();
    }

    /**
     * Gets the deadline miss handler.
     * @return A reference to an instance of {@link AsyncEventHandler} that is
     *  deadline miss handler of this.
     */
    public AsyncEventHandler getDeadlineMissHandler() {
	return missHandler;
    }

    /**
     * Sets the value of cost
     * @param cost Processing time units per period or per minimum interarrival
     * interval. On implementations which can measure the amount of time a 
     * schedulable object is executed, this value is the maximum amount of 
     * time a schedulable object receives per period or per minimum 
     * interarrival interval. On implementations which cannot measure execution
     * time, this value is used as a hint to the feasibility algorithm. 
     * On such systems it is not possible to determine when any particular 
     * object exceeds or will exceed <code>cost</code> time units in a period 
     * or interval. Equivalent to <code>RelativeTime(0,0)</code> if 
     * <code>null</code>.
     */
    public void setCost(RelativeTime cost) {
        if (cost == null) {
            this.cost.set(0,0);
        }
        else if (cost.isNegative()) {
            throw new IllegalArgumentException("can't have negative cost");
        } else {
            this.cost.set(cost);
        }
    }
    
    /**
     * Sets the cost overrun handler.
     * @param handler This handler is invoked if an invocation of the
     * {@link Schedulable} object exceeds cost. 
     * Not required for minimum implementation. If <code>null</code>, nothing 
     * happens on the overrun condition.
     * @see #setCost
     */
    public void setCostOverrunHandler(AsyncEventHandler handler) {
	this.overrunHandler = handler;
    }
    
    /**
     * Set the deadline value.
     * @param deadline The latest permissible completion time measured from
     * the release time of the associated invocation of the schedulable
     * object. Changing the deadline might not take effect after the
     * expiration of the current deadline. More detail provided in the
     * subclasses.

     */
    public void setDeadline(RelativeTime deadline) {
        if (deadline == null) {
            return; // don't know what to do. Spec needs fixing
        }
        if (deadline.isNegative()) {
            throw new IllegalArgumentException("can't have negative deadline");
        }
        else {
            this.deadline.set(deadline);
        }
    }
    
    /**
     * Sets the deadline miss handler.
     *
     * @param missHandler This handler is invoked if an invocation of the 
     * {@link Schedulable} object is still executing after the 
     * deadline has passed. Although minimum implementations do not consider
     * deadlines in feasibility calculations, they must recognize
     * variable deadlines and invoke the miss handler as appropriate. If
     * <code>null</code>, nothing happens on the miss deadline condition.
      */
    public void setDeadlineMissHandler(AsyncEventHandler missHandler) {
	this.missHandler = missHandler;
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
    public boolean setIfFeasible(RelativeTime cost, RelativeTime deadline)  {
        if (cost != null) {
            setCost(cost);
        }
        if (deadline != null) {
            setDeadline(deadline);
        }
	return true;
    }

}

