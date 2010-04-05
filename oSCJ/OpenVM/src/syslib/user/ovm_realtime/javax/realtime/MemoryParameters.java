package javax.realtime;

/**
 * Memory parameters can be given on the constructor of 
 * {@link RealtimeThread} and {@link AsyncEventHandler}. 
 * These can be used both for the purposes of admission control by
 * the scheduler and for the purposes of pacing the garbage collector
 * to satisfy all of the thread allocation rates.
 * <p>When a reference to a <code>MemoryParameters</code> object is given 
 * as a parameter to a constructor, the <code>MemoryParameters</code> 
 * object becomes bound to the object being created. Changes to the 
 * values in the <code>MemoryParameters</code> object affect the 
 * constructed object. If given to more than one constructor, then 
 * changes to the values in the <code>MemoryParameters</code> object 
 * affect <em>all</em> of the associated objects. Note that this is a
 * one-to-many relationship and <em>not</em> a many-to-many.
 *
 * <p><b>Caution:</b> This class is explicitly unsafe in multithreaded
 * situations when it is being changed.  No synchronization is done.  It
 * is assumed that users of this class who are mutating instances will be
 * doing their own synchronization at a higher level.
 *
 * <h3>OVM Notes</h3>
 * <p>Both constructors declare that they throw IllegalArgumentException, but 
 * they don't say when. I can't see any possible illegal arguments given the 
 * way the parameters are defined. Throw clause omitted.
 *
 * <p>The description of the allocationRate parameter in the constructor and 
 * in setAllocationRate states that it is "a limit on the rate of allocation 
 * in the heap". I would have expected it to say "memory area" not "heap". 
 * But the discussion on feasibility talks about the GC - so perhaps "heap" 
 * is right. Is the allocation rate control only intended to apply to heap 
 * memory or should it apply to whatever memory area the current entity is 
 * associated with? Are all memory areas (heap, immortal and scoped) supposed 
 * to allow allocation limits and allocation rate controls?
 *
 * <p>The RTSJ defines methods setMaxMemoryAreaIfFeasible and 
 * setMaxImmortalIfFeasible, yet the descriptions of these methods say 
 * nothing about feasibility tests. Are these methods meant to be simply 
 * setMaxMemoryArea and setMaxImmortal as the RI has provided? I've copied RI.
 *
 * <p>The setAllocationRateIfFeasible method doesn't make it clear what 
 * feasibility testing means in this sense. Other than the feasibility tests 
 * related to schedulers I couldn't see any further detail on what sort of 
 * feasibility test deals with allocation rate, which memory areas are 
 * required to support it, and how the absence of support for the feasibility 
 * test should be reflected in the setAllocationRateIfFeasible method. 
 * The RI simply returns false - I'll do the same.
 *
 * <p> The requirement to check if the allocation level has been exceeded by 
 * any associated thread, before setting to the new level would seem to be 
 * quite awkward for heap memory as the GC would have to track which objects 
 * were allocated by which thread and then decrement the allocation value. 
 * Was that the intention, or is this negated by allocation level only being 
 * applicable to scoped memory not heap memory?
 *
 * @author David Holmes
 */
public class MemoryParameters extends ParameterBase {

    private long maxMemoryArea = NO_MAX;
    private long maxImmortal = NO_MAX;
    private long allocationRate = NO_MAX;

    /** 
     * Specifies no maximum limit. 
     */
    public static final long NO_MAX = -1;

    
   /** 
    * Create a <code>MemoryParameters</code> object with the given values.
    *
    * @param  maxMemoryArea  A limit on the amount of memory the thread
    * may allocate in the memory area. Units are in bytes. If zero, no 
    * allocation allowed in the memory area. To specify no limit, use 
    * {@link #NO_MAX} or a value less than zero.
    * @param  maxImmortal  A limit on the amount of memory the thread 
    * may allocate in the immortal area. Units are in bytes. If zero, no 
    * allocation allowed in immortal. To specify no limit, use 
    * {@link #NO_MAX} or a value less than zero.
    *
    */   
    public MemoryParameters(long maxMemoryArea, long maxImmortal) {
        this(maxMemoryArea, maxImmortal, NO_MAX);
    }

   /** 
    * Create a <code>MemoryParameters</code> object with the given values.
    * @param  maxMemoryArea  A limit on the amount of memory the thread
    * may allocate in the memory area. Units are in bytes. If zero, no 
    * allocation allowed in the memory area. To specify no limit, use 
    * {@link #NO_MAX} or a value less than zero.
    * @param  maxImmortal  A limit on the amount of memory the thread 
    * may allocate in the immortal area. Units are in bytes. If zero, no 
    * allocation allowed in immortal. To specify no limit, use 
    * {@link #NO_MAX} or a value less than zero.
    * @param  allocationRate  A limit on the rate of allocation in the
    * heap. Units are in bytes per second. If zero, no allocation is allowed
    * in the heap. To specify no limit, use {@link #NO_MAX} or a value less 
    * than zero.
    */
    public MemoryParameters(long maxMemoryArea, long maxImmortal, 
                            long allocationRate)    {
        this.maxMemoryArea = maxMemoryArea;
        this.maxImmortal = maxImmortal;
        this.allocationRate = allocationRate;
    }

    /**
     * Get the allocation rate. Units are in bytes per second.
     * @return the allocation rate in bytes per second; a negative value
     * means no rate is set.
     */
    public long getAllocationRate() {
    	return allocationRate;
    }

    /**
     * Get the limit on the amount of memory the thread may allocate in
     * the immortal area. Units are in bytes.
     * @return the allocation limit in immortal memory  in bytes; a negative 
     * value means no limit is set.
     */
    public long getMaxImmortal()  {
        return maxImmortal;
    }

    /**
     * Get the limit on the amount of memory the thread may allocate in
     * the memory area. Units are in bytes.
     * @return the allocation limit in the memory area in bytes; a negative 
     * value means no limit is set.
     */
    public long  getMaxMemoryArea() {
        return maxMemoryArea;
    }
    
    /**
     * Sets the limit on the rate of allocation in the heap. 
     * @param  allocationRate  Units are in bytes per second. 
     * If zero, no allocation is allowed in the heap. To specify no 
     * limit, use {@link #NO_MAX} or a value less than zero.
     */
    public void setAllocationRate(long allocationRate)  {
        this.allocationRate  = allocationRate;
    }

    /**
     * Sets the limit on the rate of allocation in the heap. 
     * If this Memory-Parameters object is currently associated with one or 
     * more realtime threads that have been passed admission control, this 
     * change in allocation rate will be submitted to admission control. 
     * The scheduler (in conjunction with the garbage collector) will either 
     * admit all the effected threads with the new allocation rate, or leave 
     * the allocation rate unchanged and cause setAllocationRateIfFeasible 
     * to return false.  
     * @param allocationRate Units are in bytes per second. If zero, no
     * allocation is allowed in the heap. To specify no limit, use 
     * {@link #NO_MAX} or a value less than zero.
     *
     * @return <code>true</code> if the request was fulfilled
     */
    public boolean setAllocationRateIfFeasible(int allocationRate)  {
        return false; // What else could we do?? - DH
    }

    /**
     * Set a limit on the amount of memory the thread may allocate in the 
     * immortal area. 
     * <p>The limit can only be set if no thread associated with this has 
     * already exceeded this limit.
     * @param  maxImmortal Units are in bytes. If zero, no allocation 
     * allowed in immortal. To specify no limit, use {@link #NO_MAX} 
     * or a value less than zero.
     * @return <code>true</code> if the call succeeds.
     * </code>false</code> if any of the threads have already allocated more 
     * than the given value - in which case the call has no effect.
     */
    public boolean setMaxImmortal(long maxImmortal)  {
        // iterate over existing threads checking allocation values
        int len = schedulables.size();
        for(int i = 0; i < len ; i++) {
            RealtimeThread t = (RealtimeThread) schedulables.data[i];
            if(t.getImmortalAllocation() > maxImmortal) {
                return false;
            }
        }
        this.maxImmortal = maxImmortal;
        return true;
    }
    
    /**
     * Set a limit on the amount of memory the thread may allocate in the 
     * memory area.
     * @param maximum Units are in bytes. If zero, no allocation allowed
     * in the memory area. To specify no limit, use {@link #NO_MAX} or
     * a value less than zero.
     * @return <code>true</code> if the call succeeds.
     * </code>false</code> if any of the threads have already allocated more 
     * than the given value - in which case the call has no effect.
     */
    public boolean setMaxMemoryArea(long maximum)  {
        // iterate over existing threads checking allocation values
        int len = schedulables.size();
        for(int i = 0; i < len ; i++) {
            RealtimeThread t = (RealtimeThread) schedulables.data[i];
            if(t.getMemoryAreaAllocation() > maximum) {
                return false;
            }
        }
        this.maxMemoryArea = maximum;
        return true;
    }

}












