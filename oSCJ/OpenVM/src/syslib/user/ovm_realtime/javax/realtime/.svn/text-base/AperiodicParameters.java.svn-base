package javax.realtime;

/** 
 * This release parameter object characterizes a schedulable object that may
 * become active at any time.
 * When a reference to a <code>AperiodicParameters</code> object is given
 * as a parameter to a constructor the <code>AperiodicParameters</code> object
 * becomes bound to the object being created. Changes to the values in the 
 * <code>AperiodicParameters</code> object affect the constructed object. 
 * If given to more than one constructor then changes to the values in the 
 * <code>AperiodicParameters</code> object affect <em>all</em>
 * of the associated objects. Note that this is a one-to-many relationship and
 * <em>not</em> a many-to-many.
 *
 * <p><b>Caution:</b> This class is explicitly unsafe in multithreaded
 * situations when it is being changed.  No synchronization is done.  It
 * is assumed that users of this class who are mutating instances will be
 * doing their own synchronization at a higher level.
 *
 * <h3>OVM Notes</h3>
 * <p>The setIfFeasible method overrides ReleaseParameters.setIfFeasible and 
 * specifies that a zero cost or zero deadline leaves the value unchanged. 
 * This is inconsistent with Schedulable.setIfFeasible which uses the 
 * convention that a null value means leave unchanged. All of the 
 * setIfFeasible methods in all of the classes should adhere to the same 
 * conventions. We check for null and zero.
 */
public class AperiodicParameters extends ReleaseParameters {

    /** Create an <code>AperiodicParameters</code> object.
     *
     * @param cost Processing time per invocation. On implementations which can
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
     * If <code>null</code>, the deadline will be 
     * <code>RelativeTime(Long.MAX_VALUE, 999999)</code>.
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
    public AperiodicParameters(RelativeTime cost,
			       RelativeTime deadline,
			       AsyncEventHandler overrunHandler,
			       AsyncEventHandler missHandler) {
        super(cost, deadline, overrunHandler, missHandler);
        if (deadline == null) {
            this.deadline.set(Long.MAX_VALUE, 999999);
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
     * @param cost the proposed cost. If <code>null</code> or zero then the
     * current value is left unchanged.
     * @param deadline the proposed deadline. If <code>null</code> or zero 
     * then the current value is left unchanged.
     *
     * @return <code>true</code> if the resulting system is feasible and the 
     * changes are made.
     * <code>false</code>, if the resulting system is not feasible and no 
     * changes are made.
     */
    public boolean setIfFeasible(RelativeTime cost, RelativeTime deadline) {
        if (cost != null && 
            (cost.getMilliseconds() != 0 || cost.getNanoseconds() != 0)){
            setCost(cost);
        }
        if (deadline != null && 
            (deadline.getMilliseconds() != 0 || deadline.getNanoseconds() != 0)){
            setDeadline(deadline);
        }
        return true;
    }

}


