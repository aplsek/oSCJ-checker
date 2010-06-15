package javax.realtime;

/**
 * Class which represents the required (by the RTSJ) priority-based scheduler.
 * The default instance is the required priority scheduler which does fixed 
 * priority, preemptive scheduling.
 *
 * <h3>Implementation Notes</h3>
 * <p>This implementation does not support a feasibility test.
 * However, we should operate atomically within setIfFeasible - but given
 * an arbitrary Schedulable there is no way to do this. So the client of
 * these methods needs to ensure atomicity.
 *
 * <h3>OVM Notes</h3>
 * <p>Many of the API requirments of the RTSJ are difficult to deal with if
 * applications are allowed to provide their own Schedulers or Schedulable
 * implementations. For ease of implementation we ensure that this is not the
 * case - this is the <b>only</b> scheduler in OVM.
 * <p>The fireSchedulable method is odd. It has no defined semantics other
 * than to "fire" the schedulable. Why or when this method would be used is
 * anyone's guess. It could execute run directly (though that doesn't take
 * on the schedulable's scheduling characteristics). The RI starts the
 * schedulable if it is a thread, or creates a new thread to execute it
 * otherwise. This seems reasonable so we do it too.
 *
 */
public class PriorityScheduler extends Scheduler {


    // note these two values are NOT constants and so code that refers to
    // them will actually load the field value at runtime.

    /** 
     * The maximum priority supported by this implementation.
     */ 
    public static final int MAX_PRIORITY;

    /** 
     * The minimum priority supported by this implementation.
     */ 
    public static final int MIN_PRIORITY;

    static {
        RealtimeJavaDispatcher rtd = RealtimeJavaDispatcher.getInstance();
        MAX_PRIORITY = rtd.getMaxRTPriority();
        MIN_PRIORITY = rtd.getMinRTPriority();
    }


    /** package method for checking priority validity */
    boolean isValid(int priority) {
        return (priority >= MIN_PRIORITY && priority <= MAX_PRIORITY) ||
            (priority >= Thread.MIN_PRIORITY && priority <= Thread.MAX_PRIORITY);
    }

    /**
     * The default instance of PriorityScheduler.
     */
    // NOTE: logically this is a final field *but* as our base class
    // will access this during its static initialization and *before*
    // our static initialization has occured, we have to have the
    // instance() method check for null and assign to this instance field.
    // Hence this field can't be final. Because we know the field still gets
    // assigned during static initialization we don't need any synchronization.
    // NOTE2: The above only applies when the program logic causes this
    // class to be initialized before the Scheduler base class
    protected static volatile PriorityScheduler instance;

    /**
     * Return a reference to the default instance of PriorityScheduler.
     * @return a reference to the default instance of PriorityScheduler.
      */
    public static PriorityScheduler instance() {
        if (instance == null) {
            instance = new PriorityScheduler();
        }
        return instance;
    }


    /**
     * Construct an instance of PriorityScheduler. 
     * Applications will likely not need any other instance other than the 
     * default instance.
     */
    protected PriorityScheduler() { 
    }

    /**
     * Returns <code>true</code> as is required when no feasibility test
     * is supported.
     * @return <code>true</code>
     */
    public boolean isFeasible() {
	return true;
    }

    /**
     * Returns <code>true</code> always. We could check to see if this is
     * actually a Schedulable that is associated with this scheduler, but
     * there seems little point - until the spec is tightened.
     * @return <code>true</code>
     */
    protected boolean addToFeasibility(Schedulable schedulable) {
        return true;
    }

    /**
     * Returns <code>true</code> always. We could check to see if this is
     * actually a Schedulable that is associated with this scheduler, but
     * there seems little point - until the spec is tightened.
     * @return <code>true</code>
     */
    protected boolean removeFromFeasibility(Schedulable schedulable) {
        return true;
    }

    /**
     * @return <code>true</code> always, as there is no feasibility test
     */
    public boolean setIfFeasible(Schedulable schedulable, 
                                 ReleaseParameters release,
                                 MemoryParameters memory) {
        return setIfFeasible(schedulable, release, memory, 
                             schedulable.getProcessingGroupParameters());
    }

    /**
     * @return <code>true</code> always, as there is no feasibility test
     */
    public boolean setIfFeasible(Schedulable schedulable, 
                                 ReleaseParameters release,
                                 MemoryParameters memory,
                                 ProcessingGroupParameters group)  {
        if (schedulable != null) {
            if (release != null) {
                schedulable.setReleaseParameters(release);
            }
            if (memory != null) {
                schedulable.setMemoryParameters(memory);
            }
            if (group != null) {
                schedulable.setProcessingGroupParameters(group);
            }
        }
	return true; 
    }

    // The RI seems to do the most sensible thing here. Though it's far
    // from clear what this method should really expect to do.
    public void fireSchedulable(Schedulable schedulable) {
        // Note: the RI checks to see if this is a NHRT thread or just
        // a RT thread. The separate checks are unnecessary because a
        // NHRT thread *is* a RT thread.
        if (schedulable instanceof RealtimeThread) {
	    ((RealtimeThread)schedulable).start();
        }
	else  {
            RealtimeThread t = new RealtimeThread(
                schedulable.getSchedulingParameters(),
                schedulable.getReleaseParameters(),
                schedulable.getMemoryParameters(),
                null, // memory area
                schedulable.getProcessingGroupParameters(),
                schedulable);
            t.start();
        }
    }

    /**
     * Gets the maximum priority of the given instance of 
     * {@link java.lang.Thread}. If the given thread is scheduled by the 
     * required PriorityScheduler the maximum priority of the 
     * PriorityScheduler is returned otherwise {@link Thread#MAX_PRIORITY}
     * is returned.
     * @param thread An instance of java.lang.Thread. 
     * @return The maximum priority of the given instance of java.lang.Thread.
     * If null, the maximum priority of the required PriorityScheduler is 
     * returned.
     */
    public static int getMaxPriority(Thread thread) {
        if (thread instanceof Schedulable) {
            Schedulable s = (Schedulable) thread;
            Scheduler sched = s.getScheduler();
            if (sched instanceof PriorityScheduler) {
                return ((PriorityScheduler)sched).getMaxPriority();
            }
            else {
                return Thread.MAX_PRIORITY;
            }
        }
        else if (thread == null) { // this seems silly - should be an error
            return instance.getMaxPriority();
        }
        else {
            return Thread.MAX_PRIORITY;
        }
    }

    /**
     * Gets the minimum priority of the given instance of 
     * {@link java.lang.Thread}. If the given thread is scheduled by the 
     * required PriorityScheduler the minimum priority of the 
     * PriorityScheduler is returned otherwise {@link Thread#MIN_PRIORITY}
     * is returned.
     * @param thread An instance of java.lang.Thread. 
     * @return The minimum priority of the given instance of java.lang.Thread.
     * If null, the minimum priority of the required PriorityScheduler is 
     * returned.
     */
    public static int getMinPriority(Thread thread) {
        if (thread instanceof Schedulable) {
            Schedulable s = (Schedulable) thread;
            Scheduler sched = s.getScheduler();
            if (sched instanceof PriorityScheduler) {
                return ((PriorityScheduler)sched).getMinPriority();
            }
            else {
                return Thread.MIN_PRIORITY;
            }
        }
        else if (thread == null) { // this seems silly - should be an error
            return instance.getMinPriority();
        }
        else {
            return Thread.MIN_PRIORITY;
        }
    }

    /**
     * Gets the normal priority of the given instance of 
     * {@link java.lang.Thread}. If the given thread is scheduled by the 
     * required PriorityScheduler the normal priority of the 
     * PriorityScheduler is returned otherwise {@link Thread#NORM_PRIORITY}
     * is returned.
     * @param thread An instance of java.lang.Thread. 
     * @return The normal priority of the given instance of java.lang.Thread.
     * If null, the normal priority of the required PriorityScheduler is 
     * returned.
     */
    public static int getNormPriority(Thread thread) {
        if (thread instanceof Schedulable) {
            Schedulable s = (Schedulable) thread;
            Scheduler sched = s.getScheduler();
            if (sched instanceof PriorityScheduler) {
                return ((PriorityScheduler)sched).getNormPriority();
            }
            else {
                return Thread.NORM_PRIORITY;
            }
        }
        else if (thread == null) { // this seems silly - should be an error
            return instance.getNormPriority();
        }
        else {
            return Thread.NORM_PRIORITY;
        }
    }

    /**
     * Gets the maximum priority available for a thread managed by this 
     * scheduler.
     * @return The value of the maximum priority.
     */
    public int getMaxPriority() {
	return MAX_PRIORITY;
    }

    /**
     * Gets the minimum priority available for a thread managed by this 
     * scheduler.
     * @return The value of the minimum priority.
     */
    public int getMinPriority() {
	return MIN_PRIORITY;
    }

    /**
     * Gets the normal priority available for a thread managed by this
     * scheduler.
     * <p><code>PriorityScheduler.getNormPriority()</code> shall be set to
     * <code>((PriorityScheduler.getMaxPriority() - 
     * PriorityScheduler.getMinPriority())/3) 
     * + PriorityScheduler.getMinPriority()</code>
     *
     * @return The value of the normal priority
     */
    public int getNormPriority() {
	return ((MAX_PRIORITY - MIN_PRIORITY) / 3 ) + MIN_PRIORITY;
    }
  

    /**
     * @return The policy name (Fixed Priority) as a string.
     */
    public String getPolicyName() {
	return "Fixed Priority";
    }

    /*
     * @return If the current thread is an instance of {@link Schedulable} then
     * a new parameters object with the same value as that of the current 
     * thread; otherwise an instance of {@link PriorityParameters} with the
     * priority set to {@link #getNormPriority the normal priority}.
     */
    SchedulingParameters getDefaultSchedulingParameters() {
        Thread current = Thread.currentThread();
	SchedulingParameters temp;
        if (current instanceof Schedulable && 
            !(current instanceof RealtimeThread.VMThread))
            temp = ((Schedulable)current).getSchedulingParameters();
        else
	    temp = null;
        int prio;
        if (temp instanceof PriorityParameters)
	    prio = ((PriorityParameters) temp).getPriority();
	else
	    prio = getNormPriority();
        return new PriorityParameters(prio);
    }
}

