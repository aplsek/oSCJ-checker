package javax.realtime;

/** 
 * Importance is an additional scheduling metric that may be used by some 
 * priority-based scheduling algorithms during overload conditions to 
 * differentiate execution order among threads of the same priority.
 *
 * <p>In some real-time systems an external physical process determines
 * the period of many threads. If rate-monotonic priority assignment is
 * used to assign priorities many of the threads in the system may have 
 * the same priority because their periods are the same.  However, it is 
 * conceivable that some threads may be more important than others and in an 
 * overload situation importance can help the scheduler decide which threads 
 * to execute first.
 * 
 * <p> The base scheduling algorithm represented by {@link PriorityScheduler} 
 * is not required to use importance. However, the RTSJ strongly suggests to 
 * implementers that a fairly simple subclass of {@link PriorityScheduler} 
 * that uses importance can offer value to some real-time applications.
 *
 * <h3>OVM Notes</h3>
 * <p>We <b>do not</b> provide support for Importance at this time. However,
 * we treat ImportanceParameters just the same as PriorityParameters, and the
 * setting of importance the same as the setting of priority (sans any range 
 * check).
 * <p>The toString definition in RTSJ loses the priority information so we 
 * adopt the RI's approach and use priority:importance.
 */
public class ImportanceParameters extends PriorityParameters {

    /** the importance level represented as an integer value */
    protected int importance;
    
    /** 
     * Create an instance of <code>ImportanceParameters</code>.
     *
     * @param priority The priority assigned to the schedulable.
     * This value is used in place of {@link Thread java.lang.Thread priority}.
     * @param importance  The importance value assigned to a schedulable
     */
    public ImportanceParameters(int priority, int importance)  {
	super(priority);
	this.importance = importance;
    }
    
    /** 
     * Get the importance value.
     * @return The value of importance for the associated instance of
     * {@link Schedulable}
     */
    public int getImportance() {
	return importance;
    }
    
    /** 
     * Set the importance value.
     * @param importance the new importance value
     */
    public void setImportance(int importance) {
	this.importance = importance;
        int len = schedulables.size();
        for(int i = 0; i < len ; i++) {
            try {
                ((Schedulable)schedulables.data[i]).setSchedulingParameters(this);
            }
            catch(IllegalThreadStateException ex) {
                System.err.println(
                    "WARNING: Schedulable " + schedulables.data[i] + 
                    "was in the wrong state to set the scheduling parameters" +
                    "and is now inconsistent with those scheduling parameters!"
                    );
            }
        }
    }

    /**
     * Return a string representation of the priority and importance of this
     * ImportanceParameters object.
     * @return A strin in the form <code>priority:importance</code>
    */
    public String toString() {
	return super.toString() + ":" + importance;
    }


}
