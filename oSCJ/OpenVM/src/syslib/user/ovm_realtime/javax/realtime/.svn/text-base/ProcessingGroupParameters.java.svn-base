package javax.realtime;

/**
 * This is associated with one or more schedulable objects for which the 
 * system guarantees that the associated objects will not be given more 
 * time per period than indicated by cost.
 * For all threads with a reference to an instance of 
 * <code>ProcessingGroupParameters p</code> and a reference to an instance of 
 * {@link AperiodicParameters} no more than <code>p.cost</code> will be 
 * allocated to the execution of these threads in each interval of time given 
 * by <code>p.period</code> after the time indicated by <code>p.start</code>.
 * <p>When a reference to a <code>ProcessingGroupParameters</code> object is 
 * given as a parameter to a constructor the 
 * <code>ProcessingGroupParameters</code> object becomes bound to the object 
 * being created. Changes to the values in the 
 * <code>ProcessingGroupParameters</code> object affect the constructed object.
 * If given to more than one constructor, then changes to the values in the 
 * <code>ProcessingGroupParameters</code> object affect <em>all</em> of the 
 * associated objects. Note that this is a one-to-many relationship and
 * <em>not</em> a many-to-many.
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
 * <p>Other than this class existing we do not support the actual use of
 * ProcessingGroup parameters. It is not even clear how they are supposed to
 * be used. There was a discussion in the RTSJ mailing list in which Greg
 * Bollella gave an interesting description of how he viewed their use:
 * <p><a href="http://www.itl.nist.gov/div896/emaildir/rtj-discuss/msg00130.html" http://www.itl.nist.gov/div896/emaildir/rtj-discuss/msg00130.html>
 *
 * <p>In the constructor and setStart there are no clear details on how a 
 * null Start time should be treated.
 * <p>For cost and deadline there are no details on illegal values 
 * (eg. negative values).
 * <p>The setCostOverrunHandler method, aside from containing a typo, does not
 * describe the cost overrun handler in the same way as it is described in the
 * constructor. I believe the constructor is incorrect and I've changed it.
 *
 * <p>Similarly setDeadlineMissHandler has a different, but I think more 
 * correct, description than the constructor. So the constructor was changed.
 *
 * <p>setIfFeasible needs better documentation than the "generic" form that 
 * is used throughout the RTSJ (which really is a poor way to document these 
 * methods). [Aside: it's also unclear how an implementation should document 
 * such methods if they do not support a feasibility test].
 *
 * <p>The presence of setIfFeasible implies (to me) that all schedulables in 
 * the group should have the same scheduler. That should be a documented 
 * constraint, or at least it should be discussed.
 *
 * @author David Holmes
 */
public class ProcessingGroupParameters extends ParameterBase {

    /** 
     * the start time - should be maintained as either absolute or relative
     * consistently, but as we don't actually use it we don't care.
     */
    protected HighResolutionTime start;

    /** the period */
    protected RelativeTime period;

    /** the execution cost eg worst-case-execution-time */
    protected RelativeTime cost;

    /** the deadline */
    protected RelativeTime deadline;

    /** Handler run when cost is exceeded */
    protected AsyncEventHandler overrunHandler;

    /** Handler run when deadline is missed */
    protected AsyncEventHandler missHandler;
    
    /** 
     * Create a <code>ProcessingGroupParameters</code> object.
     *
     * @param start Time at which the first period begins.
     * @param period The period is the interval between successive unblocks of
     * {@link RealtimeThread#waitForNextPeriod waitForNextPeriod}.
     * @param cost Processing time per period.
     * @param deadline The latest permissible completion time measured from 
     * the start of the current period. Changing the deadline might not take 
     * effect until after the expiration of the current deadline.
     * @param overrunHandler This handler is invoked if the <code>run()</code>
     * method of any of the schedulable objects attempt to execute for more 
     * than cost time units in any period.
     * @param missHandler This handler is invoked if the <code>run()</code> 
     * method of any of the schedulable objects still expect to execute after 
     * the deadline has passed.
     */
    public ProcessingGroupParameters(HighResolutionTime start,
				     RelativeTime period,
				     RelativeTime cost,
				     RelativeTime deadline,
				     AsyncEventHandler overrunHandler,
				     AsyncEventHandler missHandler)
    {
	if (start == null) {
            // set to the only time guaranteed to have passed
	    this.start = new AbsoluteTime(0,0);
        }
	else {
	    this.start = start;
        }
        // there should be sanity checks on these values.
	this.period = period;
	this.cost = cost;
	this.deadline = deadline;
        // nulls are okay here
	this.overrunHandler = overrunHandler;
	this.missHandler = missHandler;
    }

    /**
     * Gets the value of cost.
     * @return the value of cost
     */
    public RelativeTime getCost() {
	return cost;
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
	return deadline;
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
     * Get the value of period.
     * @return An instance of {@link RelativeTime} that represents the value 
     * of period.
     */
    public RelativeTime getPeriod() {
	return period;
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
     * Sets the value of cost
     * @param cost the new value of cost
     */
    public void setCost(RelativeTime cost) {
	this.cost = cost;
    }
    
    /**
     * Sets the cost overrun handler.
     * @param handler This handler is invoked if the <code>run()</code> method
     * of any of the schedulable objects attempt to execute for more than 
     * cost time units in any period.
     */
    public void setCostOverrunHandler(AsyncEventHandler handler) {
	this.overrunHandler = handler;
    }
    
    /**
     * Set the deadline value.
     * @param deadline The new value for deadline
     */
    public void setDeadline(RelativeTime deadline) {
	this.deadline = deadline;
    }
    
    /**
     * Sets the deadline miss handler.
     * @param handler This handler is invoked if the <code>run()</code> 
     * method of any of the schedulable objects still expect to execute after 
     * the deadline has passed.
      */
    public void setDeadlineMissHandler(AsyncEventHandler handler) {
	this.missHandler = handler;
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
     * @param period the proposed period
     * @param cost the proposed cost
     * @param deadline the proposed deadline
     *
     * @return <code>true</code> if the resulting system is feasible and the 
     * changes are made.
     * <code>false</code>, if the resulting system is not feasible and no 
     * changes are made.
     */
    public boolean setIfFeasible(RelativeTime period, 
                                 RelativeTime cost, 
                                 RelativeTime deadline)  {
	setPeriod(period);
	setCost(cost);
	setDeadline(deadline);
        return true;
    }


    /**
     * Sets the value of the period
     * @param period the new value for period
      */
    public void setPeriod(RelativeTime period) {
	this.period = period;
    }
    
    /**
     * Sets the value of start
     * @param start the new value of start
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


}

