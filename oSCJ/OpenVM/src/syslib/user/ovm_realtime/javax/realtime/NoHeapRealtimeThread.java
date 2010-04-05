package javax.realtime;

/**
 * A <code>NoHeapRealtimeThread</code> is a specialized form of 
 * {@link RealtimeThread}. Because an instance of 
 * <code>NoHeapRealtimeThread</code> may immediately preempt any implemented
 * garbage collector logic contained in its {@link #run run()} method is 
 * never allowed to allocate or reference any object allocated in the heap 
 * nor it is even allowed to manipulate the references to objects in the heap.
 * For example, if <code>a</code> and <code>b</code> are objects in immortal 
 * memory, <code>b.p</code> is a reference to an object on the heap, and 
 * <code>a.p</code> is type compatible with <code>b.p</code>, then a
 * <code>NoHeapRealtimeThread</code> is not allowed to execute anything like 
 * the following:
 * <pre><code>
 *     a.p = b.p; b.p = null;
 * </code></pre?
 * <p>Thus, it is always safe for a <code>NoHeapRealtimeThread</code> to 
 * interrupt the garbage collector at any time, without waiting for the end 
 * of the garbage collection cycle or a defined preemption point. 
 * Due to these restrictions, a <code>NoHeapRealtimeThread</code> object
 * must be placed in a memory area such that thread logic may unexceptionally 
 * access instance variables and such that Java methods on 
 * {@link java.lang.Thread} (e.g., <code>enumerate</code> and 
 * <code>join</code>) complete normally except where execution would cause 
 * access violations. 
 * The constructors of <code>NoHeapRealtimeThread</code> require a reference 
 * to {@link ScopedMemory} or {@link ImmortalMemory}.
 * <p>When the thread is started, all execution occurs in the scope of the 
 * given memory area. Thus, all memory allocation performed with the new 
 * operator is taken from this given area.
 * <p>Parameters for constructors may be <code>null</code>. In such cases the 
 * default value will be the default value set for the particular type by the 
 * associated instance of {@link Scheduler}.
 *
 * <h3>OVM Notes</h3>
 * <p>The main functionality in NHRTT is effected by the runtime, not by
 * code here. That runtime support does not yet exist.
 * <p>The spec is inconsistent in the treatment of null constructor args.
 * We assume the same semantics as for RealtimeThread otherwise invoking
 * a super constructor makes little sense.
 */
public class NoHeapRealtimeThread extends RealtimeThread {

    /** A copy of the Runnable passed to the constructor, if any.
        We need to check its memory area when starting.
    */
    protected final Runnable myLogic;


    /**
     * Create a NoHeapRealtimeThread.
     *
     * @param scheduling A {@link SchedulingParameters} object that will be
     * associated with <code>this</code>. 
     * @param area A {@link MemoryArea} object. Must be a {@link ScopedMemory}
     *  or {@link ImmortalMemory} type. A <code>null</code> value causes an
     * {@link IllegalArgumentException} to be thrown.
     * @throws IllegalArgumentException If the memory area parameter 
     * is <code>null</code>.
     */
    public NoHeapRealtimeThread(SchedulingParameters scheduling,
                                MemoryArea area) { 
        this(scheduling, null, null, checkArea(area), null, null);
    }

    /**
     * Create a NoHeapRealtimeThread.
     *
     * @param scheduling A {@link SchedulingParameters} object that will be
     * associated with <code>this</code>. 
     * @param release A {@link ReleaseParameters} object that will be 
     * associated with <code>this</code>.
     * @param area A {@link MemoryArea} object. Must be a {@link ScopedMemory}
     *  or {@link ImmortalMemory} type. A <code>null</code> value causes an
     * {@link IllegalArgumentException} to be thrown.
     * @throws IllegalArgumentException If the memory area parameter 
     * is <code>null</code>.
     */
    public NoHeapRealtimeThread(SchedulingParameters scheduling,
                                ReleaseParameters release, 
                                MemoryArea area) {
        this(scheduling, release, null, checkArea(area), null, null);
    }


    /**
     * Create a NoHeapRealtimeThread.
     *
     * @param scheduling A {@link SchedulingParameters} object that will be
     * associated with <code>this</code>. 
     * @param release A {@link ReleaseParameters} object that will be 
     * associated with <code>this</code>.
     * @param memory A {@link MemoryParameters} object that will be associated
     * with <code>this</code>.
     * @param area A {@link MemoryArea} object. Must be a {@link ScopedMemory}
     *  or {@link ImmortalMemory} type. A <code>null</code> value causes an
     * {@link IllegalArgumentException} to be thrown.
     * @param group A {@link ProcessingGroupParameters} object that will be
     * associated with <code>this</code>. 
     * @param logic - A {@link Runnable} whose <code>run()</code> method will 
     * be executed by <code>this</code>.
     * @throws IllegalArgumentException If the memory area parameter 
     * is <code>null</code>.
     */
    public NoHeapRealtimeThread(SchedulingParameters scheduling,
                                ReleaseParameters release, 
                                MemoryParameters memory,
                                MemoryArea area, 
                                ProcessingGroupParameters group,
                                Runnable logic) {
        super(scheduling, release, memory, checkArea(area), group, logic);
        myLogic = logic;
    }


    /**
     * Helper function to validate the memory area is of the right type,
     * and that we can check this before invoking our super constructor
     * otherwise we'll have a binding leak with parameter objects if the
     * exception is thrown.
     */
    private static MemoryArea checkArea(MemoryArea area) {
        if (area == null || area instanceof ScopedMemory || 
            area instanceof ImmortalMemory) {
            return area;
        }
        else {
            throw new IllegalArgumentException("Not scoped or immortal memory area");
        }
    }

    /**
     * Checks if the <code>NoHeapRealtimeThread</code> is startable and 
     * starts it if it is.
     * Checks that the parameters associated with this NHRT object are not 
     * allocated in heap. Also checks if this object is allocated in heap. 
     * If any of them are allocated, <code>start()</code>  throws a 
     * {@link MemoryAccessError}
     * @throws MemoryAccessError If any of the parameters or 
     * <code>this</code> is allocated on heap.
    */
    public void start() {
	if (LibraryImports.reallySupportNHRTT()) {
	    MemoryArea heap = HeapMemory.instance();
	    
	    // should we also check the memory area itself and any Runnable
	    // that was passed in - seems we should, so we will - DH
	    // This is done in expanded form to make it easy to inform the
	    // user which parameter was in violation
	    if (MemoryArea.getMemoryArea(this) == heap ) 
		throw new MemoryAccessError("NHRTT allocated in heap");
	    if (MemoryArea.getMemoryArea(sParams) == heap ) 
		throw new MemoryAccessError("NHRTT scheduling parameters allocated in heap");
	    if (MemoryArea.getMemoryArea(rParams) == heap ) 
		throw new MemoryAccessError("NHRTT release parameters allocated in heap");
	    if (MemoryArea.getMemoryArea(mParams) == heap ) 
		throw new MemoryAccessError("NHRTT memory parameters allocated in heap");
	    if (MemoryArea.getMemoryArea(gParams) == heap ) 
		throw new MemoryAccessError("NHRTT group parameters allocated in heap");
	    if (MemoryArea.getMemoryArea(initArea) == heap )
		throw new MemoryAccessError("NHRTT initial memory area allocated in heap");
	    if (MemoryArea.getMemoryArea(myLogic) == heap ) 
		throw new MemoryAccessError("NHRTT Runnable logic allocated in heap");
	}

        super.start();
    }

    MemoryArea safeArea() {
        return ImmortalMemory.instance();
    }

    public String toString() {
        return "NoHeap" + super.toString() ;
    }

}

