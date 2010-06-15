/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_realtime/javax/realtime/BoundAsyncEventHandler.java,v 1.1 2004/10/15 01:53:11 dholmes Exp $
 */

package javax.realtime;

/**
 * A bound asynchronous event handler is an instance of 
 * {@link AsyncEventHandler} that is permanently bound to a thread.  
 * Bound asynchronous event handlers are meant for use in situations 
 * where the added timeliness is worth the overhead of binding the 
 * handler to a thread.
 *
 * <h3>OVM Notes</h3>
 * <p>The constructors for AEH and BoundAEH are not internally consistent
 * in the way they are defined in the RTSJ. For example, the no-arg BoundAEH
 * constructor can't delegate to the no-arg AEH constructor because they differ
 * in the treatment of the default parameter values. Similarly, the no-arg
 * constructor semantics differ from the 6 arg constructor semantics with all
 * nulls passed. (It is unclear how an unspecified noHeap value should be
 * treated: implicitly false, or inherited from the current thread.) Further
 * the semantics for Schedulable methods in AEH/BoundAEH with regard to nulls
 * are different from those defined in Schedulable and RTT. This makes it
 * awkward to forward schedulable methods direct to the bound handler thread.
 * <p><b>For now we ignore the above problems and assume that everything is
 * consistent (which we hope it will be in the RTSJ 1.01 update).</b>
 * <p>It is not clear how the bound thread will ever be asked to terminate.
 * An interrupt will do it, but what triggers the interrupt? A finalizer?
 * To avoid termination problems with the VM we make the bound thread a
 * daemon. (The RTSJ extends the VM lifecycle model to ensure events can
 * keep the VM alive, but we don't deal with that yet.
 *
 */
public class BoundAsyncEventHandler extends AsyncEventHandler {

    /**
     * Create a handler whose parameters are inherited from the current 
     * thread, if it is a {@link RealtimeThread}, or null otherwise.
     */
    public BoundAsyncEventHandler() {
        this(null, null, null, null, null, false, null);
    }

    /**
     * Create a handler with the specifed parameters.
     *
     * @param scheduling A {@link SchedulingParameters} object which will be
     * associated with the constructed instance. If <code>null</code>, 
     * <code>this</code> will be assigned the reference to the 
     * {@link SchedulingParameters} of the current thread.
     * @param release A {@link ReleaseParameters} object which will be
     * associated with the constructed instance. If <code>null</code>,
     * <code>this</code> will have no {@link ReleaseParameters}.
     * @param memory A {@link MemoryParameters} object which will be associated
     * with the constructed instance. If <code>null</code>, <code>this</code> 
     * will have no {@link MemoryParameters}.
     * @param area - The {@link MemoryArea} for <code>this</code>. 
     * If <code>null</code>, the memory area will be that of the current 
     * thread.
     * @param group A {@link ProcessingGroupParameters} object which will be
     * associated with the constructed instance. If <code>null</code>, 
     * <code>this</code> will not be associated with any processing group.
     * @param logic The {@link java.lang.Runnable} object whose 
     * <code>run()</code> method is executed by {@link #handleAsyncEvent}.
     * @param nonheap A flag meaning, when <code>true</code>, that this will 
     * have characteristics identical to a {@link NoHeapRealtimeThread}. 
     * A <code>false</code> value means this will have characteristics 
     * identical to a {@link RealtimeThread}. If <code>true</code> and the 
     * current thread is not a {@link NoHeapRealtimeThread} or a 
     * {@link RealtimeThread} executing within a {@link ScopedMemory}
     * or {@link ImmortalMemory} scope then an 
     * <code>IllegalArgumentException</code> is thrown.
     *
     * @throws IllegalArgumentException If the initial memory area is in heap 
     * memory, and the <code>noheap</code> parameter is <code>true</code>.
     * (NOTE: This is a simpler way of saying what was said above.)
     */
    public BoundAsyncEventHandler(SchedulingParameters scheduling,
                                  ReleaseParameters release,
                                  MemoryParameters memory,
                                  MemoryArea area,
                                  ProcessingGroupParameters group,
                                  boolean nonheap,
                                  Runnable logic) {
        super(scheduling, release, memory, area, group,nonheap, logic);
    }

}
