package javax.realtime;

/**  
 * A notice to the scheduler that the associated schedulable object's run 
 * method will be released aperiodically but with a minimum time between 
 * releases. When a reference to a <code>SporadicParameters</code> object 
 * is given as a parameter to a constructor, the 
 * <code>SporadicParameters</code> object becomes bound to the object being 
 * created. Changes to the values in the <code>SporadicParameters</code> 
 * object affect the constructed object. If given to more than one constructor,
 * then changes to the values in the <code>SporadicParameters</code> object 
 * affect all of the associated objects. Note that this is a one-to-many 
 * relationship and not a many-to-many.
 *
 * <p><b>Caution:</b> This class is explicitly unsafe in multithreaded 
 * situations when it is being changed. No synchronization is done. 
 * It is assumed that users of this class who are mutating instances will be 
 * doing their own synchronization at a higher level.
 *
 * <p>Correct initiation of the deadline miss and cost overrun handlers 
 * requires that the underlying system know the arrival time of each sporadic 
 * task. For an instance of {@link RealtimeThread} the arrival time is the 
 * time at which the {@link RealtimeThread#start start()} method is invoked. 
 * For other instances of {@link Schedulable} it may be required for the 
 * implmementation to save the arrival times. For instances of 
 * {@link AsyncEventHandler} with a {@link ReleaseParameters} type of 
 * <code>SporadicParameters</code> the implementation must maintain a queue of
 * monotonically increasing arrival times which correspond to the execution of 
 * the {@link AsyncEvent#fire fire()} method of the instance of 
 * {@link AsyncEvent} bound to the instance of {@link AsyncEventHandler}.
 *
 * <p> This class allows the application to specify one of four possible 
 * behaviors that indicate what to do if an arrival occurs that is closer in 
 * time to the previous arrival than the value given in this class as minimum 
 * interarrival time, what to do if, for any reason, the queue overflows, and 
 * the initial size of the queue.
 *
 * <h3>OVM Notes</h3>
 * <p>Many of the descriptions below have been modified from their RTSJ form
 * for clarity.
 * <p>Note that much of this stuff only makes sense for async events and their
 * handlers. Also note that the minimum inter-arrival time should really be
 * property of the event not the "schedulable".
 *
 * <p>For consistency with the similar methods of Schedulable, the
 * setIfFeasible methods leave a value unchanged if null is passed.
 *
 * @author David Holmes
 */

public class SporadicParameters extends AperiodicParameters {

    /**
     * Behavior object requesting that overflow of the arrival time queue
     * generate an exception.
     * <p>If an arrival time occurs and should be queued but the queue already
     * holds number of times equal to the initial queue length defined by 
     * <code>this</code> then the {@link AsyncEvent#fire fire()} method shall 
     * throw a 
     * {@link ResourceLimitError}. If the arrival time is a result of a 
     * happening to which the instance of {@link AsyncEventHandler}is bound 
     * then the arrival time is ignored.
     * <p>In other words if the event is fired programmatically then the firer
     * will get an exception (if the queue overflows then events are firing too
     * rapidly to be processed). If the event is triggerred by a happening then
     * there's no application thread to send the exception to, so it is
     * ignored (so how do you determine that your interrupts are arriving 
     * faster than expected ?) - DH
    */
    public static final String arrivalTimeQueueOverflowExcept = "arrivalTimeQueueOverflowExcept";

    /**
     * Behavior object requesting that overflow of the arrival time queue
     * cause the new arrival to be ignored.
     * <p>If an arrival time occurs and should be queued but the queue already
     * holds number of times equal to the initial queue length defined by 
     * <code>this</code> then the arrival time is ignored.
    */
    public static final String arrivalTimeQueueOverflowIgnore = "arrivalTimeQueueOverflowIgnore";

    /**
     * Behavior object requesting that overflow of the arrival time queue
     * cause the new arrival to replace the oldest existing arrival.
     * <p>If an arrival time occurs and should be queued but the queue already
     * holds a number of times equal to the initial queue length defined by 
     * <code>this</code> then the previous arrival time is overwritten by the 
     * new arrival time. However, the new time is adjusted so that the 
     * difference between it and the previous time is equal to the minimum 
     * interarrival time.
     * <p>In other words the latest arrival time replace the previous arrival
     * time. It is unclear what the MIT has to do with things in thise case.
     * - DH
     */
    public static final String arrivalTimeQueueOverflowReplace = "arrivalTimeQueueOverflowReplace";

    /**
     * Behavior object requesting that overflow of the arrival time queue
     * causes the queue to be expanded to accommodate the new arrival.
     * <p>If an arrival time occurs and should be queued but the queue already
     * holds a number of times equal to the initial queue length defined by 
     * <code>this</code> then the queue is lengthened and the arrival time 
     * is saved.
     */
    public static final String arrivalTimeQueueOverflowSave = "arrivalTimeQueueOverflowSave";

    /**
     * Behavior object requesting that a MIT violation generates an exception.
     * <p>If an arrival time for any instance of {@link Schedulable} which 
     * has <code>this</code> as its instance of {@link ReleaseParameters} 
     * occurs at a time less then the minimum interarrival time defined here 
     * then the {@link AsyncEvent#fire fire()} method shall throw
     * {@link MITViolationException}. If the arrival time is a result of a 
     * happening to which the instance of {@link AsyncEventHandler} is bound 
     * then the arrival time is ignored.
     */
    public static final String mitViolationExcept = "mitViolationExcept";

    /**
     * Behavior object requesting that a MIT violation causes the new arrival
     * time to be ignored.
     * <p>If an arrival time for any instance of {@link Schedulable} which 
     * has <code>this</code> as its instance of {@link ReleaseParameters} 
     * occurs at a time less then the minimum interarrival time defined here 
     * then the arrival time is ignored.
     */
    public static final String mitViolationIgnore = "mitViolationIgnore";

    /**
     * Behavior object requesting that a MIT violation causes the new arrival
     * time to replace the most recent arrival time.
     * <p>If an arrival time for any instance of {@link Schedulable} which 
     * has <code>this</code> as its instance of {@link ReleaseParameters} 
     * occurs at a time less then the minimum interarrival time defined here 
     * then the previous arrival time is overwritten with the new arrival time.
     * <p>In other words the two most recent firings are coalesced into a 
     * single firing that appears to have happened at the later of the two 
     * actual firings.
     */
    public static final String mitViolationReplace = "mitViolationReplace";

    /**
     * Behavior object requesting that a MIT violation causes the new arrival
     * time to replace the most recent arrival time.
     * <p>If an arrival time for any instance of {@link Schedulable} which 
     * has <code>this</code> as its instance of {@link ReleaseParameters} 
     * occurs at a time less then the minimum interarrival time defined here 
     * then the the new arrival time is added to the queue of arrival times. 
     * However, the new time is adjusted so that the difference between it and
     *  the previous time is equal to the minimum interarrival time.
     */
    public static final String mitViolationSave = "mitViolationSave";

    /** The current arrival queue overflow behavior */
    private String overflowBehavior;

    /** The initial arrival queue length */

    private int queueLength;

    /** The current mit violation behavior */
    private String mitViolationBehavior;


    /** The minimum inter-arrival time expected . */
    protected final RelativeTime mit;
     
    /** 
     * Create a <code>SporadicParameters</code> object.
     *
     * @param minInterarrival The release times of the {@link Schedulable} 
     * object will occur no closer than this interval. 
     * Must be greater than zero when entering feasibility analysis.
     *
     * @param cost - Processing time per minimum interarrival interval. On
     * implementations which can measure the amount of time 
     * a {@link Schedulable} object is executed, this value is the maximum 
     * amount of time a {@link Schedulable} object receives per interval. 
     * On implementations which 
     * cannot measure execution time, this value is used as a hint to the 
     * feasibility algorithm. On such systems it is not possible to determine 
     * when any particular object exceeds cost. Equivalent to 
     * <code>RelativeTime(0,0)</code> if <code>null</code>.
     *
     * @param deadline The latest permissible completion time measured from
     * the release time of the associated invocation of the {@link Schedulable} object.
     * For a minimum implementation for purposes of feasibility analysis, the 
     * deadline is equal to the minimum interarrival interval. Other 
     * implementations may use this parameter to compute execution eligibility.
     * If <code>null</code>, deadline will equal the minimum interarrival time.
     *
     * @param overrunHandler This handler is invoked if an invocation of the
     * {@link Schedulable} object exceeds cost. 
     * Not required for minimum implementation. 
     * If <code>null</code>, nothing happens on the overruncondition.
     *
     * @param missHandler This handler is invoked if an invocation of the 
     * {@link Schedulable} object is still executing after the 
     * deadline has passed. Although minimum implementations do not consider
     * deadlines in feasibility calculations, they must recognize
     * variable deadlines and invoke the miss handler as appropriate. If
     * <code>null</code>, nothing happens on the miss deadline condition.
     */
    public SporadicParameters(RelativeTime minInterarrival,
			      RelativeTime cost,
			      RelativeTime deadline,
			      AsyncEventHandler overrunHandler,
			      AsyncEventHandler missHandler)
    {
	super(cost, deadline, overrunHandler, missHandler);
        if (minInterarrival == null || 
            (minInterarrival.getMilliseconds() == 0 && 
             minInterarrival.getNanoseconds() == 0) ){
            throw new IllegalArgumentException("Need minimum interarrival time > 0");
        }
        else {
            this.mit = new RelativeTime(minInterarrival);
        }
        if (deadline == null) {
          this.deadline.set(this.mit);
      }
    }


    /**
     * Gets the behavior of the arrival time queue in the event of an overflow.
     * @return The behavior of the arrival time queue as a string.
     */
    public String getArrivalTimeQueueOverflowBehavior() {
	return overflowBehavior;
    }

    /**
     * Gets the initial number of elements the arrival time queue can hold.
     * @return The initial length of the queue.
     */
   public int getInitialArrivalTimeQueueLength() {
       return queueLength;
   }
    
    /** 
     * Gets the minimum interarrival time.
     * @return The minimum interarrival time.
     */
    public RelativeTime getMinimumInterarrival() {
	return new RelativeTime(mit);
    }

    /**
     * Gets the arrival time queue behavior in the event of a minimum 
     * interarrival time violation.
     * @return The minimum interarrival time violation behavior as a string. 
     */
    public String getMitViolationBehavior() {
	return mitViolationBehavior;
    }

    /**
     * Sets the behavior of the arrival time queue in the case where the 
     * insertion of a new element would make the queue size greater than the 
     * initial size given in <code>this</code>.
     *
     * @param behavior A string representing the behavior.
     */
    public void setArrivalTimeQueueOverflowBehavior(String behavior) {
        // using strings for this seems very odd. We'll assume that
        // only the defined constants are ever used for this purpose
        // and so can use == for testing - DH
        if (behavior == arrivalTimeQueueOverflowExcept ||
            behavior == arrivalTimeQueueOverflowIgnore ||
            behavior == arrivalTimeQueueOverflowReplace ||
            behavior == arrivalTimeQueueOverflowSave ) {

            overflowBehavior = behavior;
        }
        else {
            throw new IllegalArgumentException("invalid behavior");
        }
    }


    /**
     * @param deadline The latest permissible completion time measured from
     * the release time of the associated invocation of the schedulable
     * object. For a minimum implementation for purposes of feasibility 
     * analysis, the deadline is equal to the period. Other implementations 
     * may use this parameter to compute execution eligibility. 
     * If <code>null</code>, deadline will equal the minimum interarrival
     * time.
     */
    public void setDeadline(RelativeTime deadline) {
        super.setDeadline( deadline != null ? deadline : mit);
    } 


    /**
     * Sets the initial number of elements the arrival time queue can hold 
     * without lengthening the queue.
     * @param initial The initial length of the queue.
     */
    public void setInitialArrivalTimeQueueLength(int initial)  {
        // the queue length is read by a schedulable when this is bound to
        // it. Subsequent changes only affect subsequent bindings. - DH
        if (initial < 1) {
            throw new IllegalArgumentException("need initial queue length > 0");
        }
	queueLength = initial;
    }
    
    /** 
     * Sets the minimum interarrival time.
     * @param minimum The release times of the {@link Schedulable} 
     * object will occur no closer than this interval. 
     * Must be greater than zero when entering feasibility analysis.
     */
    public void setMinimumInterarrival(RelativeTime minimum) {
        if (minimum == null || 
            (minimum.getMilliseconds() == 0 && 
             minimum.getNanoseconds() == 0) ){
            throw new IllegalArgumentException("Need minimum interarrival time > 0");
        }
        else {
            this.mit.set(minimum);
        }
    }

    /**
     * Sets the behavior of the arrival time queue in the case where the new 
     * arrival time is closer to the previous arrival time than the minimum 
     * interarrival time given in <code>this</code>.
     *
     * @param behavior A string representing the behavior.
     */
    public void setMitViolationBehavior(String behavior) {
        // using strings for this seems very odd. We'll assume that
        // only the defined constants are ever used for this purpose
        // and so can use == for testing - DH
        if (behavior == mitViolationExcept ||
            behavior == mitViolationIgnore ||
            behavior == mitViolationReplace ||
            behavior == mitViolationSave ) {

            mitViolationBehavior = behavior;
        }
        else {
            throw new IllegalArgumentException("invalid behavior");
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
     * @param interarrival the proposed minimum interarrival time. 
     * If <code>null</code> the value is left unchanged.
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
    public boolean setIfFeasible(RelativeTime interarrival, 
                                 RelativeTime cost, 
                                 RelativeTime deadline)
    {
        if (interarrival != null) {
            setMinimumInterarrival(interarrival);
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


