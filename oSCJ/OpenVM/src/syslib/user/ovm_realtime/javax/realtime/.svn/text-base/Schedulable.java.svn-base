package javax.realtime;

/**
 * Handlers and other objects can be run by a {@link Scheduler} if they
 * provide a <code>run()</code> method and the methods defined below.  The
 * {@link Scheduler} uses this information to create a suitable context
 * to execute the <code>run()</code> method.
 *
 * @spec RTSJ 1.0.1 (new methods not yet enabled)
 */
public interface Schedulable extends java.lang.Runnable {
    
    /**
     * The method first performs a feasibility analysis with <code>this</code> 
     * added to the system. If the resulting system is feasible, 
     * inform the scheduler and cooperating facilities that the
     * scheduling, release, and memory parameters
     * of this instance of {@link Schedulable} should be considered in 
     * feasibility analysis until further notified. 
     * If the analysis showed that the system including <code>this</code> 
     * would not be feasible, this method does not admit <code>this</code> 
     * to the feasible set.
     * <p>
     * If the object is already included in the feasible set, do nothing.
     *
     * @return True, if <code>this</code> was admitted to the feasible set,
     *         False, if <code>this</code> was not admitted because it would 
     *         not be feasible or because there is no assigned instance of 
     *         {@link Scheduler}.
     *
     * @specbug The latter clause in the return semantics is not needed, as a
     * schedulable object can not have a null scheduler.
     *
     * @since 1.0.1 Promoted to the Schedulable interface
     */
    public boolean addIfFeasible();
  

    /** 
     * Inform the scheduler and cooperating facilities that the 
     * {@link SchedulingParameters}
     * {@link ReleaseParameters} and
     * {@link MemoryParameters}
     * of this instance of {@link Schedulable} should be considered in 
     * feasibility analysis until further notified.
     * <p>
     * If the object is already included in the feasible set, do nothing.
     *
     * @return True, if the resulting system is feasible.
     *         False, if not.
     *
     * @specbug ProcessingGroupParameters can also affect feasibility.
     */
    public boolean addToFeasibility();

    /** 
     * Gets a reference to the {@link MemoryParameters} object for this
     * schedulable object.
     *
     * @return A reference to the current {@link MemoryParameters} object.
     */
    public MemoryParameters getMemoryParameters();

    /** 
     * Gets a reference to the {@link ProcessingGroupParameters} object for 
     * this schedulable object.
     *
     * @return A reference to the current {@link ProcessingGroupParameters} 
     * object.
     */
    public ProcessingGroupParameters getProcessingGroupParameters();
  
    /** 
     * Gets a reference to the {@link ReleaseParameters} object for this
     * schedulable object.
     *
     * @return A reference to the current {@link ReleaseParameters} object.
     */
    public ReleaseParameters getReleaseParameters();
    
    /** 
     * Gets a reference to the {@link Scheduler} object for this
     * schedulable object.
     *
     * @return A reference to the current {@link Scheduler} object.
     */
    public Scheduler getScheduler();
  
    /** 
     * Gets a reference to the {@link SchedulingParameters} object for this
     * schedulable object.
     *
     * @return A reference to the current {@link SchedulingParameters} object.
     */
    public SchedulingParameters getSchedulingParameters();
  
    /** 
     * Inform the scheduler and cooperating facilities that the parameters 
     * of this instance of {@link Schedulable} should <em>not</em> be 
     * considered in  feasibility analysis until it is further notified.
     
     * @return True, if the removal was successful.
     *         False, if there is no assigned instance of {@link Scheduler}, or
     *         if the schedulable object cannot be removed from the 
     *         scheduler's feasible set; e.g., the schedulable object is not 
     *         part of the scheduler's feasible set.
     *
     * @specbug The first part of the return <tt>true</tt> case is not needed
     * as there is always an assigned instance of Scheduler
     */
    public boolean removeFromFeasibility();

    /**
     * The method first performs a
     * feasibility analysis using <code>release</code> and <code>memory</code>
     * as replacements for the matching parameter values of <code>this</code>. 
     * If the resulting system is feasible the method replaces the current
     * release and memory parameters of <code>this</code> with the new 
     * parameters.
     * <p>This change becomes effective under conditions determined by the 
     * scheduler controlling the schedulable object.  For instance, the change 
     * may be immediate or it may be delayed until the next release of the 
     * schedulable object. 
     * See the documentation for the scheduler for details.
     * <p>
     * This method does not require that the schedulable object be in the 
     * feasible set before it is called. 
     * If it is not initially a member of the feasible set
     * it will be added if the resulting schedule is feasible.
     *
     * @param release The proposed release parameters. If null, 
     *  the default value is governed by the associated scheduler 
     *  (a new object is created if the default value is not null).  
     *  (See {@link PriorityScheduler}.)
     *
     * 
     * @param memory The proposed memory parameters. If null, 
     *  the default value is governed by the associated scheduler 
     *  (a new object is created if the default value is not null).  
     *  (See {@link PriorityScheduler}.)
     *
     *
     * @return True, if the resulting system is feasible and
     *               the changes are made. 
     *         False, if the resulting system is
     *                not feasible and no changes are made.
     * 
     * @throws IllegalArgumentException Thrown when the parameter values are 
     * not compatible with the schedulable object's scheduler.
     *           
     * @throws IllegalAssignmentError Thrown if <code>this</code> object
     * cannot hold references to <code>release</code> and <code>memory</code> 
     * or <code>release</code> or <code>memory</code> cannot hold references 
     * to <code>this</code>.
     *             
     * @throws IllegalThreadStateException Thrown if the new release parameters
     * change the schedulable object from periodic scheduling to some other 
     * protocol and the schedulable object is currently waiting for the next 
     * release in {@link RealtimeThread#waitForNextPeriod} or 
     * {@link RealtimeThread#waitForNextPeriodInterruptible}.
     * 
     * @since 1.0.1 Promoted to the <code>Schedulable</code> interface.
     *
     */
    public abstract boolean setIfFeasible(ReleaseParameters release, 
                                          MemoryParameters memory);
  
    /**
     *
     * The method first performs a feasibility analysis using the new 
     * {@link ReleaseParameters}, {@link MemoryParameters}
     * and {@link ProcessingGroupParameters}
     * as replacements for the current parameters of <code>this</code>. 
     * If the resulting system is feasible the method replaces the current 
     * release, memory and processing group parameters with the corresponding 
     * replacement parameters.</p>
     * <p>This change becomes effective under conditions determined by the 
     * scheduler controlling the schedulable object.  For instance, the 
     * change may be immediate or it may be delayed until the next release of 
     * the schedulable object.  
     * See the documentation for the scheduler for details.
     * <p>
     * This method does not require that the schedulable object be in the 
     * feasible set before it is called. If it is not initially a member of 
     * the feasible set it will be added if the resulting schedule is feasible.
     * 
     * @param release The proposed release parameters. If null, 
     *  the default value is governed by the associated scheduler 
     *  (a new object is created if the default value is not null).  
     *   (See {@link PriorityScheduler}.)
     *
     * 
     * @param memory The proposed memory parameters.  If null, 
     *  the default value is governed by the associated scheduler 
     *  (a new object is created if the default value is not null).  
     *  (See {@link PriorityScheduler}.)
     *
     * 
     * @param group The proposed processing group parameters. If null, 
     *  the default value is governed by the associated scheduler 
     *  (a new object is created if the default value is not null).  
     *  (See {@link PriorityScheduler}.)
     *
     * @return True, if the resulting system is feasible and
     *               the changes are made. 
     *         False, if the resulting system is
     *                not feasible and no changes are made.
     *
     * @throws IllegalArgumentException Thrown when the parameter values are 
     * not compatible with the scheduler.
     * @throws IllegalAssignmentError  Thrown if <code>this</code> object
     * cannot hold references to all three parameter objects or
     * the parameters cannot hold references to <code>this</code>.
     * @throws IllegalThreadStateException Thrown if the new release parameters
     * change the schedulable object from periodic scheduling to some other 
     * protocol and the schedulable object is currently waiting for the next 
     * release in {@link RealtimeThread#waitForNextPeriod} or 
     * {@link RealtimeThread#waitForNextPeriodInterruptible}.
     *
     * @since 1.0.1 Promoted to the <code>Schedulable</code> interface.
     */
    public boolean setIfFeasible(ReleaseParameters release, 
				 MemoryParameters memory,
				 ProcessingGroupParameters group);

    /**
     * The method first performs a feasibility analysis using the new 
     * {@link ReleaseParameters} and 
     * {@link ProcessingGroupParameters} as replacements for the current 
     * parameters of <code>this</code>. If the resulting system is feasible, 
     * the method replaces the current <code>ReleaseParameters</code> 
     * and <code>ProcessingGroupParameters</code> with the new parameters.
     * <p>This change becomes effective under conditions determined by the 
     * scheduler controlling the schedulable object. For instance, the change 
     * may be immediate or it may be delayed until the next release of the 
     * schedulable object.  
     * See the documentation for the scheduler for details.
     *
     * <p>
     * This method does not require that the schedulable object be in the 
     * feasible set before it is called. If it is not initially a member of 
     * the feasible set it will be added if the resulting schedule is feasible.
     * 
     * @param release The proposed release parameters. If null, 
     * the default value is governed by the associated scheduler 
     * (a new object is created if the default value is not null).  
     * (See {@link PriorityScheduler}.)
     *
     * 
     * @param group The proposed processing group parameters. If null, 
     * the default value is governed by the associated scheduler 
     * (a new object is created if the default value is not null).  
     * (See {@link PriorityScheduler}.)
     *
     *
     * @return True, if the resulting system is feasible and
     *               the changes are made. 
     *         False, if the resulting system is
     *                not feasible and no changes are made.
     *
     *
     * @throws IllegalArgumentException Thrown when the parameter values are 
     * not compatible with the scheduler.
     * @throws IllegalAssignmentError Thrown if <code>this</code> object
     * cannot hold references to both parameter objects or the parameters 
     * cannot hold references to <code>this</code>.
     *
     *
     * @throws IllegalThreadStateException Thrown if the new release parameters
     * change the schedulable object from periodic scheduling to some other 
     * protocol and the schedulable object is currently waiting for the next 
     * release in {@link RealtimeThread#waitForNextPeriod} or 
     * {@link RealtimeThread#waitForNextPeriodInterruptible}.
     *
     * @since 1.0.1 Promoted to the <code>Schedulable</code> interface.
     */
    public boolean setIfFeasible(ReleaseParameters release,
				 ProcessingGroupParameters group);


    /**
     * The method first performs a
     * feasibility analysis using <code>release</code> and <code>memory</code>
     * as replacements for the matching parameter values of <code>this</code>. 
     * If the resulting system is feasible the method replaces the current
     * scheduling, release and memory parameters
     * of <code>this</code> with the corresponding replacement parameters.
     * <p>This change becomes effective under conditions determined by the 
     * scheduler controlling the schedulable object. For instance, the change 
     * may be immediate or it may be delayed until the next release of the 
     * schedulable object.  
     * See the documentation for the scheduler for details.
     *
     * <p>
     * This method does not require that the schedulable object be in the 
     * feasible set before it is called. If it is not initially a member of 
     * the feasible set it will be added if the resulting schedule is feasible.
     * 
     * @param sched The proposed scheduling parameters. If null,
     * the default value is governed by the associated scheduler 
     * (a new object is created if the default value is not null).  
     * (See {@link PriorityScheduler}.)
     *
     * @param release The proposed release parameters. If null, 
     * the default value is governed by the associated scheduler 
     * (a new object is created if the default value is not null).  
     * (See {@link PriorityScheduler}.)
     *
     * 
     * @param memory The proposed memory parameters. If null, 
     * the default value is governed by the associated scheduler 
     * (a new object is created if the default value is not null).  
     * (See {@link PriorityScheduler}.)
     *
     *
     * @return True, if the resulting system is feasible and
     *               the changes are made. 
     *         False, if the resulting system is
     *                not feasible and no changes are made.
     * 
     * @throws IllegalArgumentException Thrown when the parameter values are 
     * not compatible with the scheduler.
     * @throws IllegalAssignmentError  Thrown if <code>this</code> object
     * cannot hold references to all three parameter objects or
     * the parameters cannot hold references to <code>this</code>.
     * @throws IllegalThreadStateException Thrown if the new release parameters
     * change the schedulable object from periodic scheduling to some other 
     * protocol and the schedulable object is currently waiting for the next 
     * release in {@link RealtimeThread#waitForNextPeriod} or 
     * {@link RealtimeThread#waitForNextPeriodInterruptible}.
     *
     * @since 1.0.1
     *
     * @specbug The opening description does not mention the <tt>sched</tt>
     * parameter
     * @specbug The IllegalThreadStateException docs should refer to 
     * three parameters not two.
     */
//     public boolean setIfFeasible(SchedulingParameters sched,
//                                  ReleaseParameters release,
//                                  MemoryParameters memory);



    /**
     * The method first performs a feasibility analysis using the new 
     * {@link ReleaseParameters}, {@link MemoryParameters}
     * and {@link ProcessingGroupParameters}
     * as replacements for the current parameters of <code>this</code>. 
     * If the resulting system is feasible the method replaces the current 
     * release, memory and processing group parameters with the corresponding 
     * replacement parameters.</p>
     * <p>This change becomes effective under conditions determined by the 
     * scheduler controlling the schedulable object.  For instance, the 
     * change may be immediate or it may be delayed until the next release of 
     * the schedulable object.  
     * See the documentation for the scheduler for details.
     * <p>
     * This method does not require that the schedulable object be in the 
     * feasible set before it is called. If it is not initially a member of 
     * the feasible set it will be added if the resulting schedule is feasible.
     * 
     * @param sched The proposed scheduling parameters. If null,
     * the default value is governed by the associated scheduler 
     * (a new object is created if the default value is not null).  
     * (See {@link PriorityScheduler}.)
     *
     *
     * @param release The proposed release parameters. If null, 
     *  the default value is governed by the associated scheduler 
     *  (a new object is created if the default value is not null).  
     *   (See {@link PriorityScheduler}.)
     *
     * 
     * @param memory The proposed memory parameters.  If null, 
     *  the default value is governed by the associated scheduler 
     *  (a new object is created if the default value is not null).  
     *  (See {@link PriorityScheduler}.)
     *
     * 
     * @param group The proposed processing group parameters. If null, 
     *  the default value is governed by the associated scheduler 
     *  (a new object is created if the default value is not null).  
     *  (See {@link PriorityScheduler}.)
     *
     * @return True, if the resulting system is feasible and
     *               the changes are made. 
     *         False, if the resulting system is
     *                not feasible and no changes are made.
     *
     * @throws IllegalArgumentException Thrown when the parameter values are 
     * not compatible with the scheduler.
     * @throws IllegalAssignmentError  Thrown if <code>this</code> object
     * cannot hold references to all three parameter objects or
     * the parameters cannot hold references to <code>this</code>.
     * @throws IllegalThreadStateException Thrown if the new release parameters
     * change the schedulable object from periodic scheduling to some other 
     * protocol and the schedulable object is currently waiting for the next 
     * release in {@link RealtimeThread#waitForNextPeriod} or 
     * {@link RealtimeThread#waitForNextPeriodInterruptible}.
     *
     * @since 1.0.1
     *
     * @specbug The opening description does not mention the <tt>sched</tt>
     * parameter.
     * @specbug The IllegalThreadStateException docs should refer to 
     * four parameters not three.
     */
//     public boolean setIfFeasible(SchedulingParameters sched,
//                                  ReleaseParameters release, 
// 				 MemoryParameters memory,
// 				 ProcessingGroupParameters group);

    /**
     * Sets the memory parameters associated with this instance of 
     * <code>Schedulable</code>.
     * <p>This change becomes effective under conditions determined by the 
     * scheduler controlling the schedulable object.  For instance, the change 
     * may be immediate or it may be delayed until the next release of the 
     * schedulable object.  
     * See the documentation for the scheduler for details.
     * <p>Since this affects the
     * constraints expressed in the memory parameters of the
     * existing schedulable objects, this may change the
     * feasibility of the current schedule.
     * 
     * @param memory A {@link MemoryParameters} object which will become the 
     *             memory parameters
     *             associated with <code>this</code> after the method call.
     *             If null, 
     *             the default value is governed by the associated scheduler 
     *             (a new object is created if the default value is not null).
     *              (See {@link PriorityScheduler}.)
     * @throws IllegalArgumentException Thrown if <code>memory</code>
     * is not compatible with the schedulable object's scheduler.
     * @throws IllegalAssignmentError Thrown if the schedulable object
     * cannot hold a reference to <code>memory</code>, 
     * or if <code>memory</code> cannot hold a reference to 
     * this schedulable object instance.
     */
    public void setMemoryParameters(MemoryParameters memory);

    /**
     * The method first performs a feasibility analysis using the given 
     * memory parameters as replacements for the memory parameters of 
     * <code>this</code> If the resulting system is feasible the method 
     * replaces the current memory parameters of <code>this</code> with
     * the new memory parameters.
     * 
     * <p>This change becomes effective under conditions determined by the 
     * scheduler controlling the schedulable object.  For instance, the change 
     * may be immediate or it may be delayed until the next release of the 
     * schedulable object.  
     * See the documentation for the scheduler for details.
     * <p>
     * This method does not require that the schedulable object be in the 
     * feasible set before it is called. If it is not initially a member of 
     * the feasible set it will be added if the resulting schedule is feasible.
     * 
     * @param memory The proposed memory parameters. If null, 
     * the default value is governed by the associated scheduler 
     * (a new object is created if the default value is not null).  
     * (See {@link PriorityScheduler}.)
     *
     *
     * @return True, if the resulting system is feasible and
     *         the changes are made. 
     *         False, if the resulting system is
     *         not feasible and no changes are made.
     *
     * @throws IllegalArgumentException Thrown when <code>memory</code>
     * is not compatible with the scheduler.
     * @throws IllegalAssignmentError Thrown if <code>this</code> object
     * cannot hold a reference to <code>memory</code> or
     * <code>memory</code> cannot hold a reference to <code>this</code>.
     *
     */
    public boolean setMemoryParametersIfFeasible(MemoryParameters memory);

    /**
     * Sets the {@link ProcessingGroupParameters} of <code>this</code>.
     * <p>This change becomes effective under conditions determined by the 
     * scheduler controlling the schedulable object.  For instance, the change 
     * may be immediate or it may be delayed until the next release of the 
     * schedulable object.  
     * See the documentation for the scheduler for details.
     * <p>Since this affects the
     * constraints expressed in the processing group parameters of the
     * existing schedulable objects, this may change the
     * feasibility of the current schedule.
     *
     * @param group A {@link ProcessingGroupParameters} object which will take
     * effect at the next release of <code>this</code>. If null, 
     * the default value is governed by the associated scheduler 
     * (a new object is created if the default value is not null).  
     * (See {@link PriorityScheduler}.)
     *
     * @throws IllegalArgumentException Thrown when <code>group</code>
     * is not compatible with the scheduler for this schedulable object.
     * @throws IllegalAssignmentError Thrown if <code>this</code> object
     * cannot hold a reference to <code>group</code> or
     * <code>group</code> cannot hold a reference to <code>this</code>.
     *
     * @specbug The parameter docs for <tt>group</tt> should not say that
     * it takes effect on the next release.
     */
    public void setProcessingGroupParameters(ProcessingGroupParameters group);

    /**
     * The method first performs a feasibility analysis using the new 
     * {@link ProcessingGroupParameters} as a replacement for the 
     * current parameters of <code>this</code>. If the resulting system is 
     * feasible the method replaces the current 
     * {@link ProcessingGroupParameters} with the new parameters. 
     * The changes take place at the schedulable object's next release.
     * <p>This change becomes effective under conditions determined by the 
     * scheduler controlling the schedulable object.  For instance, the change 
     * may be immediate or it may be delayed until the next release of the 
     * schedulable object.  
     * See the documentation for the scheduler for details.
     * <p>
     * This method does not require that the schedulable object be in the 
     * feasible set before it is called. If it is not initially a member of 
     * the feasible set it will be added if the resulting schedule is feasible.
     * 
     * @param group The {@link ProcessingGroupParameters} object. If null, 
     * the default value is governed by the associated scheduler 
     * (a new object is created if the default value is not null).  
     * (See {@link PriorityScheduler}.)
     *
     * @return True, if the resulting system is feasible and
     *         the changes are made. 
     *         False, if the resulting system is
     *         not feasible and no changes are made.
     *     
     *
     * @throws IllegalArgumentException Thrown when <code>group</code>
     * is not compatible with the scheduler.
     * @throws IllegalAssignmentError Thrown if <code>this</code> object
     * cannot hold a reference to <code>group</code> or
     * <code>group</code> cannot hold a reference to <code>this</code>.
     *              *
     * @specbug The first paragraph should not say that the change
     * takes effect on the next release.
     */
    public boolean setProcessingGroupParametersIfFeasible(ProcessingGroupParameters group);
  
    /**
     * Sets the release parameters associated with this instance of 
     * <code>Schedulable</code>.
     * <p>Since this affects the
     * constraints expressed in the release parameters of the
     * existing schedulable objects, this may change the
     * feasibility of the current schedule.
     * <p>This change becomes effective under conditions determined by the 
     * scheduler controlling the schedulable object. For instance, the change 
     * may be immediate or it may be delayed until the next release of the 
     * schedulable object. 
     * The different properties of the release parameters may
     * take effect at different times.
     * See the documentation for the scheduler for details.
     *
     *
     * @param release A {@link ReleaseParameters} object which will become the 
     * release parameters associated with this after the method call.
     * If null, the default value is governed by the associated scheduler 
     * (a new object is created if the default value is not null).  
     *   (See {@link PriorityScheduler}.)
     *
     *
     *
     * @throws IllegalArgumentException Thrown when <code>release</code>
     * is not compatible with the scheduler.
     * @throws IllegalAssignmentError Thrown if <code>this</code> object
     * cannot hold a reference to <code>release</code> or
     * <code>release</code> cannot hold a reference to <code>this</code>.  
     *              
     * @throws IllegalThreadStateException Thrown if the new release parameters
     * change the schedulable object from periodic scheduling to some other 
     * protocol and the schedulable object is currently waiting for the next 
     * release in {@link RealtimeThread#waitForNextPeriod} or 
     * {@link RealtimeThread#waitForNextPeriodInterruptible}.
     *
     * @specbug The parameter docs for <tt>release</tt> should not say that
     * it takes effect at the end of the call.
     */
    public void setReleaseParameters(ReleaseParameters release);

    /**
     * The method first performs a feasibility analysis using the new 
     * {@link ReleaseParameters} as a replacement for the current parameters 
     * of <code>this</code>. If the resulting system is feasible the 
     * method replaces the current {@link ReleaseParameters} with the new 
     * parameters.
     * 
     * <p>
     * This method does not require that the schedulable object be in the 
     * feasible set before it is called. If it is not initially a member of 
     * the feasible set it will be added if the resulting schedule is feasible.
     * 
     * <p>This change becomes effective under conditions determined by the 
     * scheduler controlling the schedulable object. For instance, the change 
     * may be immediate or it may be delayed until the next release of the 
     * schedulable object. 
     * The different properties of the release parameters may
     * take effect at different times.
     * See the documentation for the scheduler for details.
     *
     * @param  release The {@link ReleaseParameters} object. 
     * If null, default release parameters for the associated scheduler 
     * are used (a new object is created if the default value is not null).
     *
     *
     * @return True, if the resulting system is feasible and
     *         the changes are made. 
     *         False, if the resulting system is
     *         not feasible and no changes are made.
     *
     * @throws IllegalArgumentException Thrown when <code>release</code>
     * is not compatible with the scheduler.
     * @throws IllegalAssignmentError Thrown if <code>this</code> object
     * cannot hold a reference to <code>release</code> or
     * <code>release</code> cannot hold a reference to <code>this</code>.
     *
     * @throws IllegalThreadStateException Thrown if the new release parameters
     * change the schedulable object from periodic scheduling to some other 
     * protocol and the schedulable object is currently waiting for the next 
     * release in {@link RealtimeThread#waitForNextPeriod} or 
     * {@link RealtimeThread#waitForNextPeriodInterruptible}.
     */
    public boolean setReleaseParametersIfFeasible(ReleaseParameters release);

    /** 
     * Sets the reference to the Scheduler object.  The timing of the change
     * is under the control of the new scheduler.  In the case of the default
     * scheduler, the change is made immediately.
     * <p>
     * If this schedulable object has been admitted to the feasible set 
     * (e.g., by {@link #addToFeasibility}) it remains in the feasible set 
     * after the change in scheduler. This change in the scheduler may affect 
     * the feasibility of the current schedule.     
     *
     * @param scheduler A reference to the scheduler that will manage 
     * execution of this schedulable object. <code>Null</code> is not a 
     * permissible value.
     *
     * @throws IllegalArgumentException Thrown when <code>scheduler</code>
     * is null, or the schedulable object's existing parameter values are not 
     * compatible with the new scheduler.
     *
     * @throws IllegalAssignmentError Thrown if the schedulable object
     * cannot hold a reference to <code>scheduler</code>.
     *          
     * @throws SecurityException Thrown if the caller is not permitted to set 
     * the scheduler for this schedulable object.
     *
     * @specbug It is not clear how the feasible set of the old and new 
     * scheduler are updated.
     * @specbug The first paragraph should not saying anything about the
     * default scheduler and is wrong anyway.
     * @specbug It would seem to be difficult for the new scheduler to control
     * when it takes control of the SO. Schedulers would need to be aware of
     * each other's requirements and cooperate to perform the handover.
     */
    public void setScheduler(Scheduler scheduler);
    
    /** 
     * Sets the scheduler and associated parameter objects.  The timing of the
     * change is under the
     * control of the new scheduler.  In the case of the default scheduler, 
     * the scheduler and scheduling parameters take control immediately, 
     * and the release and processing group parameters take effect the next 
     * time the schedulable is released.
     *
     * <p>
     * If this schedulable object has been admitted to the feasible set 
     * (e.g., by {@link #addToFeasibility}) it remains in the feasible set 
     * after the change in scheduler. This change in the scheduler may affect 
     * the feasibility of the current schedule.     
     *
     * @param scheduler A reference to the scheduler that will manage the 
     * execution of this schedulable object. Null is not a permissible value.
     *
     * @param scheduling A reference to the {@link SchedulingParameters} which 
     * will be associated with <code>this</code>. If null, 
     * the default value is governed by <code>scheduler</code> 
     * (a new object is created if the default value is not null).  
     * (See {@link PriorityScheduler}.)
     *
     * @param release A reference to the {@link ReleaseParameters} which will 
     * be associated with <code>this</code>. If null, 
     * the default value is governed by <code>scheduler</code> 
     * (a new object is created if the default value is not null).  
     * (See {@link PriorityScheduler}.)
     *
     *
     * @param memoryParameters A reference to the {@link MemoryParameters} 
     * which will be associated with <code>this</code>. If null, 
     * the default value is governed by <code>scheduler</code> 
     * (a new object is created if the default value is not null).  
     * (See {@link PriorityScheduler}.)
     *
     * @param group A reference to the {@link ProcessingGroupParameters} which 
     * will be associated with <code>this</code>. If null, 
     * the default value is governed by <code>scheduler</code> 
     * (a new object is created).  (See {@link PriorityScheduler}.)
     *
     *
     * @throws IllegalArgumentException Thrown when scheduler is null or the
     * parameter values are not compatible with the scheduler.
     * @throws IllegalAssignmentError Thrown if <code>this</code> object
     * cannot hold references to all the parameter objects or
     * the parameters cannot hold references to <code>this</code>.
     * @throws IllegalThreadStateException Thrown if the new release parameters
     * change the schedulable object from periodic scheduling to some other 
     * protocol and the schedulable object is currently waiting for the next 
     * release in {@link RealtimeThread#waitForNextPeriod} or 
     * {@link RealtimeThread#waitForNextPeriodInterruptible}.
     *
     * @throws SecurityException Thrown if the caller is not permitted to set 
     * the scheduler for this schedulable object.
     *
     * @specbug The first paragraph should not saying anything about the
     * default scheduler and is wrong anyway.
     * @specbug The second paragraph regarding feasibility should refer to
     * changes in the scheduler or parameter objects.
     * @specbug It would seem to be difficult for the new scheduler to control
     * when it takes control of the SO. Schedulers would need to be aware of
     * each other's requirements and cooperate to perform the handover.
     */
    public void setScheduler(Scheduler scheduler, 
			     SchedulingParameters scheduling, 
			     ReleaseParameters release, 
			     MemoryParameters memoryParameters, 
			     ProcessingGroupParameters group);
	
    
    /** 
     * Sets the scheduling parameters associated with this instance of 
     * <code>Schedulable</code>. 
     *   
     * <p>Since this affects the scheduling parameters of 
     * the existing schedulable objects, this may change the 
     * feasibility of the current schedule.
     * <p>This change becomes effective under conditions determined by the 
     * scheduler controlling the schedulable object.  For instance, the change 
     * may be immediate or it may be delayed until the next release of the 
     * schedulable object.  
     * See the documentation for the scheduler for details.
     *
     * @param scheduling A reference to the {@link SchedulingParameters} 
     * object. If null, 
     * the default value is governed by the associated scheduler 
     * (a new object is created if the default value is not null).  
     * (See {@link PriorityScheduler}.)
     *
     *
     * @throws IllegalArgumentException Thrown when <code>scheduling</code>
     * is not compatible with the scheduler.
     * @throws IllegalAssignmentError  Thrown if <code>this</code> object
     * cannot hold a reference to <code>scheduling</code> or
     * <code>scheduling</code> cannot hold a reference to <code>this</code>.
     *
     *     
     */
    public void setSchedulingParameters(SchedulingParameters scheduling);

    /**
     * The method first performs a feasibility analysis using the given 
     * scheduling parameters as replacements for the scheduling parameters of 
     * <code>this</code>. If the resulting system is feasible the method 
     * replaces the current scheduling parameters of <code>this</code> with
     * the new scheduling parameters.
     * <p>This change becomes effective under conditions determined by the 
     * scheduler controlling the schedulable object.  For instance, the change 
     * may be immediate or it may be delayed until the next release of the 
     * schedulable object.  
     * See the documentation for the scheduler for details.
     * <p>
     * This method does not require that the schedulable object be in the 
     * feasible set before it is called. If it is not initially a member of 
     * the feasible set it will be added if the resulting schedule is feasible.
     * 
     * @param scheduling The proposed scheduling parameters.
     * If null, the {@link SchedulingParameters} are set to the 
     * default <code>SchedulingParameters</code> for the associated 
     * scheduler (if the default value is not null a new object is created).
     *
     *
     * @return True, if the resulting system is feasible and
     *         the changes are made. 
     *         False, if the resulting system is
     *         not feasible and no changes are made.
     *
     * @throws IllegalArgumentException Thrown when <code>scheduling</code>
     * is not compatible with the scheduler.
     * @throws IllegalAssignmentError Thrown if <code>this</code> object
     * cannot hold a reference to <code>scheduling</code> or
     * <code>scheduling</code> cannot hold a reference to <code>this</code>.
     *
     *
     */
    public boolean setSchedulingParametersIfFeasible(SchedulingParameters scheduling);
    
}
