package javax.realtime;

/**
 * An instance of <code>Scheduler</code> manages the execution of schedulable 
 * objects and may implement a feasibility algorithm. The feasibility 
 * algorithm may determine if the known set of schedulable objects, given 
 * their particular execution ordering (or priority assignment), is a feasible
 *  schedule.
 * <p>Subclasses of <code>Scheduler</code> are used for alternative scheduling
 * policies and should define an <code>instance()</code> class method to 
 * return the default instance of the subclass. The name of the subclass 
 * should be descriptive of the policy, allowing applications to deduce the 
 * policy available for the scheduler obtained via 
 * {@link Scheduler#getDefaultScheduler} (e.g., <code>EDFScheduler</code>).
 *
 * <h3>OVM Notes</h3>
 * <p>It is not clear under what circumstances addToFeasibility will return
 * true or false. Can a schedulable be considered more than once?? 
 * In principle a Schedulable like an AEH could be associated with multiple
 * threads and hence impact the feasibility analysis in multiple ways.
 * It is also not clear what happens if the Schedulable is not associated 
 * with this scheduler.
 * <p>The same questions need to be answered for removeFromFeasibility.
 * <p>setDefaultcheduler should treat passing null as an error.
 * <p>The setIfFeasible methods should be abstract.
 */
public abstract class Scheduler {
    
    /** The current set default scheduler */
    private static volatile Scheduler defaultScheduler = PriorityScheduler.instance();

    /**
     * Create an instance of Scheduler
     */
    protected Scheduler() {}

    /** Internal locking object for use when atomicty is needed across
     * scheduler methods.
     */
    Object lock = new Object();

    /**
     * Inform the scheduler and cooperating facilities that the resource 
     * demands (as expressed in the associated instances of 
     * {@link SchedulingParameters} , {@link ReleaseParameters} , 
     * {@link MemoryParameters}, and {@link ProcessingGroupParameters}) of 
     * the given instance of {@link Schedulable} will be considered in the 
     * feasibility analysis of the associated Scheduler until further notice. 
     * Whether the resulting system is feasible or not, the addition is 
     * completed.
     *
     * @param schedulable The schedulable to add to the feasibility 
     * consideration
     * @return <code>true</code> if the addition was successful. 
     * <code>false</code> if not.
     */
    protected abstract boolean addToFeasibility(Schedulable schedulable);

    /**
     * Trigger the execution of a schedulable object (like an 
     * {@link AsyncEventHandler}).
     * @param schedulable The schedulable object to make active.
     */
    public abstract void fireSchedulable(Schedulable schedulable);

    /**
     * Return a reference to the default scheduler.
     * @return a reference to the default scheduler.
     */
    public static Scheduler getDefaultScheduler() {
	return defaultScheduler;
    }

    /**
     * Gets a string representing the policy of <code>this<code> scheduler.
     * @return A String object which is the name of the scheduling policy 
     * used by this scheduler.
     */
    public abstract String getPolicyName();

    /** 
     * Queries the system about the feasibility of the set of scheduling and 
     * release characteristics currently being considered.
     * @return <code>true</code> if the system is feasible and 
     * <code>false</code> otherwise.
     */
    public abstract boolean isFeasible();

    /**
     * Inform the scheduler and cooperating facilities that the resource 
     * demands (as expressed in the associated instances of 
     * {@link SchedulingParameters} , {@link ReleaseParameters} , 
     * {@link MemoryParameters}, and {@link ProcessingGroupParameters}) of 
     * the given instance of {@link Schedulable} should no longer be 
     * considered in the 
     * feasibility analysis of the associated Scheduler.
     * Whether the resulting system is feasible or not, the removal is 
     * completed.
     *
     * @param schedulable The schedulable to remove from the feasibility 
     * consideration
     * @return <code>true</code> if the removal  was successful. 
     * <code>false</code> if not.
     */
    protected abstract boolean removeFromFeasibility(Schedulable schedulable);

    /**
     * Sets the default scheduler. This is the scheduler given to instances 
     * of {@link RealtimeThread} when they are constructed.
     * The default scheduler is set to the required {@link PriorityScheduler} 
     * at startup.
     * @param scheduler The Scheduler that becomes the default scheduler
     * assigned to new threads. If null nothing happens.
      */
    public static void setDefaultScheduler(Scheduler scheduler) {
        if (scheduler != null) {
            defaultScheduler = scheduler;
        }
    }

    /**
     * Replaces the existing parameter objects of the given {@link Schedulable}
     * , with the supplied ones, if the
     * resulting characteristics yield a feasible system.
     * <p>This method first performs a feasibility analysis using the new 
     * scheduling characteristics as replacements for the matching scheduling 
     * characteristics of the given Schedulable. 
     * If the resulting system is feasible the method replaces the current 
     * scheduling characteristics with the new scheduling characteristics.
     *
     * @param schedulable The instance of Schedulable for which the changes are
     * proposed.
     * @param release The proposed release parameters.
     * @param memory The proposed memory parameters.
     *
     * @return <code>true</code> if the resulting system is feasible and the 
     * changes are made.
     * <code>false</code> if the resulting system is not feasible and no 
     * changes are made.
     */
    public boolean setIfFeasible(Schedulable schedulable,
                                 ReleaseParameters release,
                                 MemoryParameters memory) {
        /* It is impossible for this abstract base class to know how to
         * set the various parameters without knowing about the concrete
         * schedulable type. You might think that you could just delegate to
         * the Schedulable.setIfFeasible method but that could be written in 
         * terms of *this* method and a concrete scheduler without a 
         * feasibility test might not override this method.
         * This method should be abstract.
         */
        // luckily we don't do a feasibility test so we can set directly.
        synchronized(lock) {
            schedulable.setReleaseParameters(release);
            schedulable.setMemoryParameters(memory);
        }
        return true;
    }


    /**
     * Replaces the existing parameter objects of the given {@link Schedulable}
     * , with the supplied ones, if the
     * resulting characteristics yield a feasible system.
     * <p>This method first performs a feasibility analysis using the new 
     * scheduling characteristics as replacements for the matching scheduling 
     * characteristics of the given Schedulable. 
     * If the resulting system is feasible the method replaces the current 
     * scheduling characteristics with the new scheduling characteristics.
     *
     * @param schedulable The instance of Schedulable for which the changes are
     * proposed.
     * @param release The proposed release parameters.
     * @param memory The proposed memory parameters.
     * @param group The proposed processing group parameters.
     *
     * @return <code>true</code> if the resulting system is feasible and the 
     * changes are made.
     * <code>false</code> if the resulting system is not feasible and no 
     * changes are made.
     */
    public boolean setIfFeasible(Schedulable schedulable,
                                 ReleaseParameters release,
                                 MemoryParameters memory,
                                 ProcessingGroupParameters group) {
        /* It is impossible for this abstract base class to know how to
         * set the various parameters without knowing about the concrete
         * schedulable type. You might think that you could just delegate to
         * the Schedulable.setIfFeasible method but that could be written in 
         * terms of *this* method and a concrete scheduler without a 
         * feasibility test might not override this method.
         * This method should be abstract.
         */
        // luckily we don't do a feasibility test so we can set directly.
        synchronized(lock) {
            schedulable.setReleaseParameters(release);
            schedulable.setMemoryParameters(memory);
            schedulable.setProcessingGroupParameters(group);
        }
        return true;

    }

    /* package-scoped implementation methods */

    /* The RTSJ specifies that a newly created RTThread gets the parameter
       objects of it's creator, if it is a Schedulable, else the default
       parameter objects of the Scheduler that will manage it. The RTSJ
       does not define any such methods for querying the scheduler so we
       add them. The process is clearer if the default policy of a Scheduler
       is to return a copy of the parameters associated with the current
       thread if it is a schedulable, and some specific object (perhaps null)
       otherwise.
    */

    /** 
     * Return the default {@link SchedulingParameters} object for 
     * <code>this</code>
     * @return If the current thread is an instance of {@link Schedulable} then
     * a new parameters object with the same value as that of the current 
     * thread; otherwise <code>null</code>, as scheduling parameters are not 
     * required
     */
    SchedulingParameters getDefaultSchedulingParameters() {
        Thread current = Thread.currentThread();
        if (current instanceof Schedulable && 
            !(current instanceof RealtimeThread.VMThread)) {
            SchedulingParameters temp = 
                ((Schedulable)current).getSchedulingParameters();
            return temp == null ? null : (SchedulingParameters)temp.clone();
        }
        else {
            return null;
        }
    }

    /** 
     * Return the default {@link ReleaseParameters} object for 
     * <code>this</code>
     * @return If the current thread is an instance of {@link Schedulable} then
     * a new parameters object with the same value as that of the current 
     * thread; otherwise <code>null</code>, as release parameters are not 
     * required
     */
    ReleaseParameters getDefaultReleaseParameters() {
        Thread current = Thread.currentThread();
        if (current instanceof Schedulable && 
            !(current instanceof RealtimeThread.VMThread)) {
            ReleaseParameters temp = 
                ((Schedulable)current).getReleaseParameters();
            return temp == null ? null : (ReleaseParameters)temp.clone();
        }
        else {
            return null;
        }
    }

    /** 
     * Return the default {@link MemoryParameters} object for 
     * <code>this</code>
     * @return If the current thread is an instance of {@link Schedulable} then
     * a new parameters object with the same value as that of the current 
     * thread; otherwise <code>null</code>, as memory parameters are not 
     * required
     */
    MemoryParameters getDefaultMemoryParameters() {
        Thread current = Thread.currentThread();
        if (current instanceof Schedulable && 
            !(current instanceof RealtimeThread.VMThread)) {
            MemoryParameters temp = ((Schedulable)current).getMemoryParameters();
            return temp == null ? null : (MemoryParameters)temp.clone();
        }
        else {
            return null;
        }
    }

    /** 
     * Return the default {@link ProcessingGroupParameters} object for 
     * <code>this</code>
     * @return If the current thread is an instance of {@link Schedulable} then
     * a new parameters object with the same value as that of the current 
     * thread; otherwise <code>null</code>, as processing group parameters 
     * are not required
     */
    ProcessingGroupParameters getDefaultProcessingGroupParameters() {
        Thread current = Thread.currentThread();
        if (current instanceof Schedulable && 
            !(current instanceof RealtimeThread.VMThread)) {
            ProcessingGroupParameters temp = 
                ((Schedulable)current).getProcessingGroupParameters();
            return temp == null ? null : 
                (ProcessingGroupParameters)temp.clone();
        }
        else {
            return null;
        }
    }

}


