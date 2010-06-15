/*
 * $Header:
 * /p/sss/cvs/OpenVM/src/syslib/user/ovm_classpath/javax/realtime/AsyncEventHandler.java,v
 * 1.1 2003/03/29 23:32:00 dholmes Exp $
 */
package javax.realtime;

/**
 * An asynchronous event handler encapsulates code that executes at some time
 * after an instance of {@link AsyncEvent}to which it is bound occurs.
 * <p>
 * It is essentially a {@link java.lang.Runnable} with a set of parameter
 * objects, making it very much like a {@link RealtimeThread}. The expectation
 * is that there may be thousands of events, with corresponding handlers,
 * averaging about one handler per event. The number of unblocked (i.e.,
 * scheduled) handlers is expected to be relatively small.
 * <p>
 * It is guaranteed that multiple firings of an event handler will be
 * serialized. It is also guaranteed that (unless the handler explicitly
 * chooses otherwise) for each firing of the handler, there will be one
 * execution of the {@link #handleAsyncEvent} method.
 * <p>
 * Instances of <code>AsyncEventHandler</code> with a release parameter of
 * type {@link SporadicParameters}have a list of release times which
 * correspond to the occurance times of instances of {@link AsyncEvent}to
 * which it is bound. The minimum interarrival time specified in
 * {@link SporadicParameters}is enforced as defined there. Unless the handler
 * explicitly chooses otherwise there will be one execution of the code in
 * {@link #handleAsyncEvent}for each entry in the list. The i <sup>th</sup>
 * execution of {@link #handleAsyncEvent}will be released for scheduling at
 * the time of the i <sup>th</sup> entry in the list.
 * <p>
 * There is no restriction on what handlers may do. They may run for a long or
 * short time, and they may block. (Note: blocked handlers may hold system
 * resources.)
 * <p>
 * Normally, handlers are bound to an execution context dynamically, when the
 * instance of {@link AsyncEvent} to which they are bound occurs. This can
 * introduce a (small) time penalty. For critical handlers that can not afford
 * the expense, and where this penalty is a problem, use a
 * {@link BoundAsyncEventHandler}.
 * <p>
 * The semantics for memory areas that were defined for realtime threads apply
 * in the same way to instances of <code>AsyncEventHandler</code>. They may
 * inherit a scope stack when they are created, and the single parent rule
 * applies to the use of memory scopes for instances of
 * {@link AsyncEventHandler} just as it does in realtime threads.
 * 
 * <h3>OVM Notes</h3>
 * <p>We are forced to use a bound thread per AEH. The reason for this is that
 * otherwise we leak memory when using, for example, a periodic timer handler
 * running in scope. This will be revisted when we redesign the implementation
 * to support the 1.0.1 semantics.
 * <p>
 * The deadlines for async event handlers are not currently tracked correctly.
 * Whenever <tt>getAndDecrementPendingFireCount</tt> is called we should
 * reset the deadline relative to the next event firing. Problem is we don't
 * know when that event actually fired.
 * 
 * <p>Many of the semantics here need updating to conform to RTSJ 1.0.1. In
 * places we rely on the uniformity provided by 1.0.1 to fix inconstencies
 * between, for example, constructor variants, and the default values used
 * for null parameters.
 *
 * 
 */
public class AsyncEventHandler implements Schedulable {

    /** Our bound thread */
    final RealtimeThread handler;
    
    /** Was the bound thread already started ? (which means, was it already bound to an event?) */
    protected boolean handlerStarted = false;

    /** Internal lock object for synchronization */
    protected final Object lock = new Object();

    /** Flag that tells the handler there is work to do */
    boolean noWork = true; // access while holding 'lock'

    /** The fire count for this event */
    protected int fireCount = 0; // access while holding 'lock'

    /** The Runnable bound to this event handler - if any */
    protected final Runnable logic;


    /** The logic executed by our handler */
    Runnable handlerLogic = new Runnable() {
            public void run() {
                while(!Thread.interrupted()) {
                    synchronized(AsyncEventHandler.this.lock) {
                        while (AsyncEventHandler.this.noWork) {
                            try { 
                                AsyncEventHandler.this.lock.wait();
                            }
                            catch (InterruptedException ie) {
                                return;
                            }
                        }
                        AsyncEventHandler.this.noWork = true;
                    }
                    // we don't need or want to hold the lock while processing
                    AsyncEventHandler.this.run();
                }
            }
        };


    /**
     * Create an instance of <code>AsyncEventHandler</code> whose
     * {@link SchedulingParameters} are inherited from the current thread and
     * does not have either {@link ReleaseParameters} or
     * {@link MemoryParameters}.
     */
    public AsyncEventHandler() {
        this(null, null, null, null, null, false,null);
    }

    /**
     * Create an instance of <code>AsyncEventHandler</code> whose
     * {@link SchedulingParameters} are inherited from the current thread and
     * does not have either {@link ReleaseParameters} or
     * {@link MemoryParameters}.
     * 
     * @param logic  The {@link java.lang.Runnable} object whose 
     * <code>run()</code> method is executed by {@link #handleAsyncEvent}.
     */
    public AsyncEventHandler(Runnable logic) {
        this(null, null, null, null, null, false, logic);
    }

    /**
     * Create an instance of <code>AsyncEventHandler</code> with the specifed
     * parameters.
     * 
     * @param scheduling
     * A {@link SchedulingParameters}object which will be
     * associated with the constructed instance. If <code>null</code>,
     * <code>this</code> will be assigned a clone of
     * {@link SchedulingParameters} of the current thread.
     * {@link SchedulingParameters} of the current thread.
     * @param release A {@link ReleaseParameters} object which will be 
     * associated with the constructed instance. If <code>null</code>,
     * <code>this</code> will have no {@link ReleaseParameters}.
     * @param memory A {@link MemoryParameters} object which will be associated
     * with the constructed instance. If <code>null</code>,
     * <code>this</code> will have no {@link MemoryParameters}.
     * @param area  The {@link MemoryArea} for <code>this</code>. 
     * If <code>null</code>, the memory area will be that of the 
     * current thread.
     * @param group A {@link ProcessingGroupParameters} object which will be
     * associated with the constructed instance. If <code>null</code>,
     * <code>this</code> will not be associated with any processing group.
     * @param nonHeap A flag meaning, when <code>true</code>, that this will
     * have characteristics identical to a {@link NoHeapRealtimeThread}. 
     * A <code>false</code> value means this will have characteristics 
     * identical to a {@link RealtimeThread}. If <code>true</code> and the
     * current thread is not a {@link NoHeapRealtimeThread} or a
     * {@link RealtimeThread} executing within a {@link ScopedMemory} or 
     * {@link ImmortalMemory}scope then an 
     * <code>IllegalArgumentException</code> is thrown.
     * 
     * @throws IllegalArgumentException
     * If the initial memory area is in heap memory, and the 
     * <code>noheap</code> parameter is <code>true</code>. 
     * (NOTE: This is a simpler  way of saying what was said above.)
     */
    public AsyncEventHandler(
                             SchedulingParameters scheduling,
                             ReleaseParameters release,
                             MemoryParameters memory,
                             MemoryArea area,
                             ProcessingGroupParameters group,
                             boolean nonHeap) {
        this(scheduling, release, memory, area, group, nonHeap, null);
    }

    /**
     * Create an instance of <code>AsyncEventHandler</code> with the specifed
     * parameters.
     * 
     * @param scheduling
     * A {@link SchedulingParameters}object which will be
     * associated with the constructed instance. If <code>null</code>,
     * <code>this</code> will be assigned a clone of
     * {@link SchedulingParameters} of the current thread.
     * @param release
     * A {@link ReleaseParameters}object which will be associated
     * with the constructed instance. If <code>null</code>,
     * <code>this</code> will have no {@link ReleaseParameters}.
     * @param memory
     * A {@link MemoryParameters}object which will be associated
     * with the constructed instance. If <code>null</code>,
     * <code>this</code> will have no {@link MemoryParameters}.
     * @param area 
     * The {@link MemoryArea}for <code>this</code>. If <code>null</code>,
     * the memory area will be that of the current thread.
     * @param group
     * A {@link ProcessingGroupParameters}object which will be
     * associated with the constructed instance. If <code>null</code>,
     * <code>this</code> will not be associated with any processing group.
     * @param logic
     * The {@link java.lang.Runnable}object whose <code>run()</code>
     * method is executed by {@link #handleAsyncEvent}.
     */
    public AsyncEventHandler(
                             SchedulingParameters scheduling,
                             ReleaseParameters release,
                             MemoryParameters memory,
                             MemoryArea area,
                             ProcessingGroupParameters group,
                             Runnable logic) {
        this(scheduling, release, memory, area, group, false, logic);
    }


    /**
     * Create an instance of <code>AsyncEventHandler</code> with the specifed
     * parameters.
     * 
     * @param scheduling
     * A {@link SchedulingParameters}object which will be
     * associated with the constructed instance. If <code>null</code>,
     * <code>this</code> will be assigned a clone of
     * {@link SchedulingParameters} of the current thread.
     * @param release
     * A {@link ReleaseParameters}object which will be associated
     * with the constructed instance. If <code>null</code>,
     * <code>this</code> will have no {@link ReleaseParameters}.
     * @param memory
     * A {@link MemoryParameters}object which will be associated
     * with the constructed instance. If <code>null</code>,
     * <code>this</code> will have no {@link MemoryParameters}.
     * @param area -
     * The {@link MemoryArea}for <code>this</code>. If <code>null</code>,
     * the memory area will be that of the current thread.
     * @param group
     * A {@link ProcessingGroupParameters}object which will be
     * associated with the constructed instance. If <code>null</code>,
     * <code>this</code> will not be associated with any
     * processing group.
     * @param logic
     * The {@link java.lang.Runnable}object whose <code>run()</code>
     * method is executed by {@link #handleAsyncEvent}.
     * @param nonHeap
     * A flag meaning, when <code>true</code>, that this will
     * have characteristics identical to a
     * {@link NoHeapRealtimeThread}. A <code>false</code> value
     * means this will have characteristics identical to a
     * {@link RealtimeThread}. If <code>true</code> and the
     * current thread is not a {@link NoHeapRealtimeThread}or a
     * {@link RealtimeThread}executing within a
     * {@link ScopedMemory}or {@link ImmortalMemory}scope then an
     * <code>IllegalArgumentException</code> is thrown.
     * 
     * @throws IllegalArgumentException
     * If the initial memory area is in heap memory, and the <code>noheap</code>
     * parameter is <code>true</code>. (NOTE: This is a simpler
     * way of saying what was said above.)
     */
    public AsyncEventHandler(
                             SchedulingParameters scheduling,
                             ReleaseParameters release,
                             MemoryParameters memory,
                             MemoryArea area,
                             ProcessingGroupParameters group,
                             boolean nonHeap,
                             Runnable logic) {

        if (nonHeap) {
            handler = new NoHeapRealtimeThread(scheduling, release, memory, 
                                               area, group, handlerLogic);
        }
        else {
            handler = new RealtimeThread(scheduling, release, memory, 
                                         area, group, handlerLogic);
        }

        this.logic = logic;

        // if the handler thread isn't a daemon then we have to do some
        // tricky stuff to figure out when to terminate it - eg. finalization
        // But it's not obvious that being a daemon thread will always be
        // the right thing as in an event drive application we always need a
        // non-daemon thread to keep the VM alive. If the events come from a
        // timer then that is already covered. If they come from happenings
        // (which we don't support yet) then we probably need a thread that
        // is started when a handler is registered for a happening, and which
        // terminates when the last handler is deregistered. - DH 24 Nov. 2003
        
        // TK: as of RTSJ 1.0.2 (and maybe earlier as well), the default is
        // true, but it can be modified from the async event
        
        handler.setDaemon(true);

        // for simplicity we start the thread now
        
        // TK: we cannot start it now, because we need to set the daemon flag off/on from
        // the async event in AE's .setDaemon
        
        // handler.start();

    }
    
    protected synchronized void startIfNotStarted() {
        if (!handlerStarted) {
            handlerStarted = true;
            handler.start();
        }
    }
    
    public void setDaemon(boolean on) throws IllegalThreadStateException {
        handler.setDaemon(on);
    }
    
    public boolean isDaemon() {
        return handler.isDaemon();
    }

    /**
     * Create an instance of <code>AsyncEventHandler</code> whose parameters
     * are inherited from the current thread, if it is a {@link RealtimeThread}
     * or <code>null</code> otherwise.
     * 
     * @param nonHeap
     * A flag meaning, when <code>true</code>, that this will
     * have characteristics identical to a
     * {@link NoHeapRealtimeThread}. A <code>false</code> value
     * means this will have characteristics identical to a
     * {@link RealtimeThread}. If <code>true</code> and the
     * current thread is not a {@link NoHeapRealtimeThread}or a
     * {@link RealtimeThread}executing within a
     * {@link ScopedMemory}or {@link ImmortalMemory}scope then an
     * <code>IllegalArgumentException</code> is thrown.
     * 
     * @throws IllegalArgumentException
     * If the initial memory area is in heap memory, and the <code>noheap</code>
     * parameter is <code>true</code>. (NOTE: This is a simpler
     * way of saying what was said above.)
     */
    public AsyncEventHandler(boolean nonHeap) {
        this(null, null, null, null, null, nonHeap, null);
    }

    /**
     * Create an instance of <code>AsyncEventHandler</code> whose parameters
     * are inherited from the current thread, if it is a {@link RealtimeThread}
     * or <code>null</code> otherwise.
     * 
     * @param logic
     * The {@link java.lang.Runnable}object whose <code>run()</code>
     * method is executed by {@link #handleAsyncEvent}.
     * @param nonHeap
     * A flag meaning, when <code>true</code>, that this will
     * have characteristics identical to a
     * {@link NoHeapRealtimeThread}. A <code>false</code> value
     * means this will have characteristics identical to a
     * {@link RealtimeThread}. If <code>true</code> and the
     * current thread is not a {@link NoHeapRealtimeThread}or a
     * {@link RealtimeThread}executing within a
     * {@link ScopedMemory}or {@link ImmortalMemory}scope then an
     * <code>IllegalArgumentException</code> is thrown.
     * 
     * @throws IllegalArgumentException
     * If the initial memory area is in heap memory, and the <code>noheap</code>
     * parameter is <code>true</code>. (NOTE: This is a simpler
     * way of saying what was said above.)
     */
    public AsyncEventHandler(Runnable logic, boolean nonHeap) {
        this(null, null, null, null, null, nonHeap, logic);
    }

    /* additional 'thread-like' AEH methods */

    /**
     * This is an accessor method for the instance of {@link MemoryArea}
     * associated with <code>this</code>.
     * 
     * @return The instance of {@link MemoryArea}which is the current area for
     *               <code>this</code>.
     */
    public MemoryArea getMemoryArea() {
        return handler.getMemoryArea();
    }


    // override all Schedulable methods to forward to the handler thread

    public boolean addIfFeasible() {
        return handler.addIfFeasible();
    }

    public boolean addToFeasibility() {
        return handler.addToFeasibility();
    }

    public MemoryParameters getMemoryParameters() {
        return handler.getMemoryParameters();
    }

    public ProcessingGroupParameters getProcessingGroupParameters() {
        return handler.getProcessingGroupParameters();
    }

    public ReleaseParameters getReleaseParameters() {
        return handler.getReleaseParameters();
    }

    public Scheduler getScheduler() {
            return handler.getScheduler();
    }

    public SchedulingParameters getSchedulingParameters() {
        return handler.getSchedulingParameters();
    }

    public boolean removeFromFeasibility() {
        return handler.removeFromFeasibility();
    }

    public boolean setIfFeasible(ReleaseParameters release,
                                 MemoryParameters memory) {
	return handler.setIfFeasible(release, memory);
    }

    public boolean setIfFeasible(ReleaseParameters release,
                                 MemoryParameters memory,
                                 ProcessingGroupParameters group) {
        return handler.setIfFeasible(release, memory, group);
    }

    public boolean setIfFeasible(ReleaseParameters release,
                                 ProcessingGroupParameters group) {
	return handler.setIfFeasible(release, group);
    }

    // FIXME: this isn't supposed to take effect until the next release
    // of the handler!
    public void setMemoryParameters(MemoryParameters parameters) {
        handler.setMemoryParameters(parameters);
    }

    public boolean setMemoryParametersIfFeasible(MemoryParameters memory) {
        return handler.setMemoryParametersIfFeasible(memory);
    }

    public void setProcessingGroupParameters(ProcessingGroupParameters parameters) {
        handler.setProcessingGroupParameters(parameters);
    }

    public boolean setProcessingGroupParametersIfFeasible(ProcessingGroupParameters group) {
        return handler.setProcessingGroupParametersIfFeasible(group);
    }
    
    public void setReleaseParameters(ReleaseParameters release) {
        handler.setReleaseParameters(release);
    }

    public boolean setReleaseParametersIfFeasible(ReleaseParameters release) {
        return handler.setReleaseParametersIfFeasible(release);
    }


    public void setScheduler(Scheduler scheduler) {
        handler.setScheduler(scheduler);
    }

    public void setScheduler(Scheduler scheduler,
                             SchedulingParameters scheduling,
                             ReleaseParameters release, 
                             MemoryParameters memory,
                             ProcessingGroupParameters group) {
        handler.setScheduler(scheduler, scheduling, release, memory, group);
    }

    public void setSchedulingParameters(SchedulingParameters parameters) {
        handler.setSchedulingParameters(parameters);
    }

    public boolean setSchedulingParametersIfFeasible(SchedulingParameters scheduling)  {
        return handler.setSchedulingParametersIfFeasible(scheduling);
    }


    /* methods related to maintaining the fire count */

    /**
     * This is an accessor method for <code>fireCount</code>. This method
     * atomically sets the value of <code>fireCount</code> to zero and
     * returns the value from before it was set to zero. This may used by
     * handlers for which the logic can accommodate multiple firings in a
     * single execution. The general form for using this is:
     * 
     * <pre><code>public void handleAsyncEvent() { int numberOfFirings = getAndClearPendingFireCount();
     *          <handle the events>}</code></pre>
     * 
     * @return The value held by <code>fireCount</code> prior to setting the
     *               value to zero
     */
    protected final int getAndClearPendingFireCount() {
        synchronized (lock) {
            int temp = fireCount;
            fireCount = 0;
            return temp;
        }
    }

    /**
     * This is an accessor method for <code>fireCount</code>. This method
     * atomically decrements,by one, the value of <code>fireCount</code> (if
     * it was greater than zero) and returns the value from before the
     * decrement. This method can be used in the {@link #handleAsyncEvent}
     * method to handle multiple firings:
     * 
     * <pre><code>public void handleAsyncEvent() {
     *         <setup>do {
     *             <handle the event>} while(getAndDecrementPendingFireCount()>0); }</code></pre>
     * 
     * 
     * <p>
     * This construction is necessary only in the case where one wishes to
     * avoid the setup costs since the framework guarantees that
     * {@link #handleAsyncEvent}will be invoked the appropriate number of
     * times.
     * 
     * @return The value held by <code>fireCount</code> prior to decrementing
     *               it by one.
     */
    protected int getAndDecrementPendingFireCount() {
        synchronized (lock) {
            int temp = fireCount;
            if (fireCount > 0) {
                --fireCount;
            }
            return temp;
        }
    }

    /**
     * This is an accessor method for <code>fireCount</code>. This method
     * atomically increments, by one, the value of <code>fireCount</code> and
     * returns the value from before the increment.
     * 
     * @return The value held by <code>fireCount</code> prior to incrementing
     *               it by one.
     */
    protected int getAndIncrementPendingFireCount() {
        synchronized (lock) {
            return fireCount++;
        }
    }

    /**
     * This is an accessor method for <code>fireCount</code>. The <code>fireCount</code>
     * field nominally holds the number of times associated instances of
     * {@link AsyncEvent}have occurred that have not had the method
     * {@link #handleAsyncEvent}invoked. Due to accessor methods the
     * application logic may manipulate the value in this field for application
     * specific reasons.
     * 
     * @return The value held by <code>fireCount</code>.
     */
    protected final int getPendingFireCount() {
        synchronized (lock) {
            return fireCount;
        }
    }

    /* Now the actual event handling related method */

    /**
     * This method holds the logic which is to be executed when associated
     * instances of {@link AsyncEvent}occur. If this handler was constructed
     * using an instance of {@link java.lang.Runnable}as an argument to the
     * constructor, then that instance�s <code>run()</code> method will be
     * invoked from this method. This method will be invoked repeatedly while
     * <code>fireCount</code> is greater than zero.
     */
    public void handleAsyncEvent() {
        if (logic != null) {
            logic.run();
        }
    }

    /**
     * Used by the asynchronous event mechanism, see {@link AsyncEvent}. This
     * method invokes {@link #handleAsyncEvent}repeatedly while the fire count
     * is greater than zero. Applications cannot override this method and
     * should thus override {@link #handleAsyncEvent}in subclasses with the
     * logic of the handler.
     */
    public final void run() {
        while (getAndDecrementPendingFireCount() > 0) {
            try {
                handleAsyncEvent();
            } catch (Throwable t) {
                // there needs to be a logging mechanism for this
                try {
                    System.err.println(t);
                    t.printStackTrace();
                }
                catch(Throwable t2) {
                    // FIXME: need to log using raw I/O
                }
            }
        }
        // note that fireCount should be zero but the event might have
        // just fired again
    }

    /**
     * Internal hook from {@link AsyncEvent}that causes this handler to be
     * run. Ideally there should be a threadpool for this but for now we just
     * create new threads: either RT or NHRT as appropriate. 
     */
    /* package */
    /**
     * Internal hook from {@link AsyncEvent} that causes this handler to
     * be released.
     */
    void releaseHandler() {
        // first increment the fire count. We only release if the
        // previous count was zero, otherwise the handler is already
        // running
        synchronized(lock) {
            if ( getAndIncrementPendingFireCount() == 0 ) {
                noWork = false;
                lock.notify();
            }
        }
    }


}
