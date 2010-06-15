package javax.realtime;
import java.lang.reflect.Field;
import org.ovmj.java.Opaque;
import org.ovmj.util.PragmaNoBarriers;
/**
 * Class <code>RealtimeThread</code> extends {@link java.lang.Thread} and 
 * includes classes and methods to get and set parameter objects, manage the 
 * execution of those threads with {@link ReleaseParameters} type of 
 * {@link PeriodicParameters} , and manage waiting.
 *<p>A <code>RealtimeThread</code> object must be placed in a memory area such 
 * that thread logic may unexceptionally access instance variables and such 
 * that Java methods on <code>java.lang.Thread</code> (e.g., enumerate and 
 * join) complete normally except where such execution would cause access 
 * violations.
 * <p>Parameters for constructors may be <code>null</code>. In such cases the 
 * default value will be the default value set for the particular type by the 
 * associated instance of {@link Scheduler}.
 * <h3>Implementation Dependent Behaviour<h3>
 * <p>This implementation does not support tracking of cost overruns.
 * <p>This implementation does not support memory allocation tracking & control.
 *
 * <h3>OVM Notes</h3>
 * 
 * <p>The memory placement requirements are far from clear, so we'll have to
 * figure this out as we go.
 *
 * <p>We allow priority changes at any time. This is allowed in RTSJ 1.0.1
 *
 * <h4>Synchronization</h4>
 * <p>All methods that require atomicity must acquire the monitor lock of the
 * local {@link #rtLock} object. 
 * <p>The RTSJ states that all parameter objects are not thread-safe and the
 * caller is responsible for synchronization. But the runtime doesn't know how
 * the application will perform synchronization, nor does the application know
 * how the runtime performs synchronization.
 * This effectively means correct synchronization is <b>impossible</b> - 
 * these class should be immutable. Within the runtime methods that use 
 * public mutating methods of parameter objects must synchronize on that 
 * object. We suggest
 * that applications do the same thing. The package-private register/unregister
 * methods take care of their own synchronization.
 * <p>Methods that require an atomic action across several scheduler methods
 * must synchronize on the {@link Scheduler#lock} object.
 * <p>Always acquire the local lock object first to avoid deadlock.
 *
 * <h4>Deadline Monitoring</h4>
 * <p><b>DEADLINE MISS SEMANTICS ARE INCORRECT - ALL FUNCTIONALITY NEEDS
 * UPDATING TO CLARIFIED RTSJ 1.0.1 SEMANTICS - July 21, 2004</b>
 * <p>The initial approach to deadline monitoring is simple but heavyweight.
 * Each thread that has a deadline miss handler set when it is started (or
 * regardless if it is a periodic thread) has an associated one-shot timer
 * that will fire when the deadline is reached. When the thread starts the 
 * watchdog starts. As the thread terminates the watchdog is disabled and
 * destroyed. For periodic threads we disable the watchdog when we sleep in
 * waitForNextPeriod and reschedule it when we wakeup. Deadline monitoring
 * is always active for periodic threads, even if no handler is set, as we
 * have to return false from waitForNextPeriod.
 * <p>This approach is heavy because each timer has an associated thread
 * and so we get two threads per thread when watching deadlines.
 * <p>Using this approach, setting a deadline miss handler after a thread
 * has started, only affects periodic threads and then only on the next
 * period. The semantics for dynamically changing the release parameters are
 * not clear. If we become periodic then we create a watchdog and the the
 * first deadline relative to when we set the release parameters. If we stop
 * being periodic we disable the watchdog.
 * <p>It is not completely clear how the missing of deadlines should be dealt
 * with in {@link #waitForNextPeriod}. We differ from the RI in that we
 * only return false once per missed deadline, whereas the RI
 * calculates the number of periods missed  and returns false for the same 
 * number of {@link #waitForNextPeriod} invocations. For example, if we had
 * a period of 10ms and we invoked waitForNextPeriod after 50ms, then
 * the RI will cause WFNP to return false for 5 invocations (or more if by
 * the time you make those five calls another 10ms interval has elapsed). 
 * In contrast we return false once. The wording in the spec is ambiguous.
 * Our approach is simpler and seems much cleaner (though there are
 * implementation issues as the RI approach avoids the need to 
 * reschedule the timer, instead using a periodic timer that is kept in sync
 * with the expected release times).
 * <p>The deadlines for async event handlers are not currently tracked 
 * correctly. Whenever <tt>getAndDecrementPendingFireCount</tt> is called
 * we should reset the deadline relative to the next event firing. Problem is
 * we don't know when that event actually fired.
 * <h3>To Do</h3>
 * <p>Connect interrupt to async interruption
 * <p>The setIfFeasible methods are all broken due to possible side-effects 
 * when setting parameter objects. The entire strategy for dealing with this
 * needs to be thought out again. Feasibility tests should work with dummy
 * objects where changing a parameter doesn't have a real affect - we can then
 * change the parameter in the real object once we know we should.
 * <p>We don't alter the deadline monitoring action if the release parameters
 * are changed. There is no clear action to take when this occurs.
 * <p>Need to check the deadline miss behaviour of waitForNextPeriod.
 *
 * @author David Holmes
 *
 */
public class RealtimeThread extends Thread implements Schedulable {

    /** the real-time dispatcher */
    static final RealtimeJavaDispatcher rtd = 
                    RealtimeJavaDispatcher.getInstance();

    // package access for parameters to allow NHRTT to access them.

    /** The release parameters for this thread */
    volatile ReleaseParameters rParams;

    /** The scheduling parameters for this thread */
    volatile SchedulingParameters sParams;

    /** The processing group parameters for this thread */
    volatile ProcessingGroupParameters gParams;

    /** The memory parameters for this thread */
    volatile MemoryParameters mParams;

    /** The scheduler bound to this thread - we only have one type in OVM */
    volatile PriorityScheduler scheduler;

    /** The initial memory area associated with this thread, or with the 
     * currently executing <code>Schedulable</code>. 
     */
    // NOTE: this is accessed reflectively by the VM
    final MemoryArea initArea;

    final int initAreaIndex;  // the index of the initial memory area in the
                              // initial scope stack of this thread. As
                              // returned by getInitialMemoryAreaIndex
    
    /** The current scope stack. Only accessed directly during construction,
        startup, termination and finalization.
     */
    private ScopeStack scopeStack;

    /** Return the scope stack of this thread. This is overridden by some
        system threads to do special things.
    */
    ScopeStack getScopeStack() {
        return scopeStack;
    }

    void setScopeStack(ScopeStack newStack) throws PragmaNoBarriers {
        scopeStack = newStack;
    }

    /** set to true if the initArea had to be pushed at construct time.
        Otherwise the initArea has to be upRef'ed at start time.
     */
    boolean didIMAPushOnConstruct = false;

    /** Flag to indicate if waitForNextPeriod is enabled */
    private volatile boolean schedulePeriodic = true;

    /** 
     * The absolute time this thread should be released
     * (accounting for the start specified in the release parameters).
     * A periodic thread is initially scheduled relative to this time.
     */
    private long startTimeNanos = 0;  


    /**
     * The absolute time at which the current period frame started. This is
     * initially the start time, for a periodic thread, and is updated if
     * the period is changed or periodic release parameters are set.
     */
    private long startPeriodFrame = 0;

    /**
     * The current period, in nanoseconds, for a periodic thread. This is
     * set when a periodic thread is started and is updated on a call to
     * waitForNextPeriod. When periodic parameters are set, or changed, 
     * dynamically, the new period is not recognised until waitForNextPeriod
     * is called, and even then it takes affect after the subsequent release.
     * We use a value of -1 to indicate an uninitialized value
     */
    private long periodNanos = -1;

    /**
     * Flag set by the deadline monitoring software if this thread misses the
     * deadline for this release of the thread. It is cleared in 
     * {@link #waitForNextPeriod}.
     */
    private volatile boolean deadlineMissed = false;

    /** Flag for turning on all the debug statements for deadline monitoring */
    // make non-static and non-final if you want to change per-thread
    static final boolean DEBUG_DEADLINES = false;

    static final boolean DEBUG_SCOPESTACK = false;

    static final boolean ASSERTS_ON = false; // changed by FP

    /**
     * Internal locking object. This cannot be made different to the
     * locking object used in our parent.  We use Thread.priority to
     * hold both normal java priorities and realtime priorities, hence
     * we need to lock the Thread object in setSchedulingParameters.
     */
    private Object rtLock = this;

    /**
     * termination flag to cover discontinuity between termination code here
     * and isAlive() returning false, which is controlled by Thread,runThread.
     */
    private boolean terminating = false;

    private static final Field rt_priority;
    static {
	try {
	    rt_priority = Thread.class.getDeclaredField("priority");
	    rt_priority.setAccessible(true);
	} catch (NoSuchFieldException e) {
	    throw new LinkageError(e.toString());
	}
    }

    void setPriorityInternal(int prio) {
	try {
	    rt_priority.setInt(this, prio);
	} catch (IllegalAccessException e) {
	    throw new Error(e);
	}
    }

    /**
     * Special timer class that in addition to triggering the handler(s)
     * directly sets the deadline missed flag for this thread. This is simpler
     * than defining a custom handler to set the flag and then forward to the
     * real handler because we don't have to keep all of the schedulable's
     * parameters in sync when the real handler changes.
     * <p>It also provides an atomic reset method that reschedules and enables
     * the timer.
     */
    class DeadlineMonitor extends OneShotTimer {
        DeadlineMonitor(HighResolutionTime start, 
                        Clock clock, 
                        AsyncEventHandler handler){
            super(start, clock, handler);
        }

        RealtimeThread myThread = RealtimeThread.this;

        void fireInternal() {
            if (RealtimeThread.DEBUG_DEADLINES) {
                System.out.println(
                    "watchdog timer FIRED for thread: " + myThread.getName() + 
                    " at " + Clock.RealtimeClock.getCurrentTimeNanos() );
            }
            synchronized(myThread.rtLock) {
                myThread.deadlineMissed = true;
                // if we have a handler set then deschedule periodic
                if (handlers.size() > 0 
                    && myThread.rParams instanceof PeriodicParameters) {
                    if (RealtimeThread.DEBUG_DEADLINES) {
                        System.out.println("Descheduling periodic");
                    }
                    myThread.deschedulePeriodic();
                }
            }
            super.fireInternal();
        }

        /** used for resets */
        AbsoluteTime deadline = new AbsoluteTime();

        /**
         * Reset function that takes an absolute time in nanoseconds and
         * uses that to reschedule the timer and enable it atomically.
         */
        void reset(long absoluteNanos) {
            deadline.setDirect(absoluteNanos);
            if (RealtimeThread.DEBUG_DEADLINES) {
                System.out.println("Rescheduling watchdog for " + 
                                   myThread.getName() + " with: " + 
                                   absoluteNanos);
            }
            synchronized(lock) {
                // if this deadline has already passed it will cause an
                // immediate fire
                reschedule(deadline);
                if (!isRunning())
                    start();
                else
                    enable();
            }
        }
    };

    /**
     * Package private class for internal VM threads. These threads don't
     * use parameter objects at all, typically run at system level priority
     * and can do anything they want :). There's a no-heap version defined 
     * inside NHRT that extends this one. No Schedulable methods, or public
     * RTT methods should be called on these threads, but it is okay to call
     * thread methods like start(), interrupt(), join(), if needed
     */
    static class VMThread extends RealtimeThread {

        // The memory area aspects of VMThread are still unclear.
        // It is "transparent" when creating new RTT's or AEH in terms of
        // parameter objects, but the scope stack is copied

        VMThread() {
            super(0); // special super constructor call
	    setPriorityInternal(RealtimeJavaDispatcher.SYSTEM_RT_PRIORITY);
            // all parameters are null
        }

        VMThread(Runnable logic) {
            super(logic); // special super constructor call
	    setPriorityInternal(RealtimeJavaDispatcher.SYSTEM_RT_PRIORITY);
            // all parameters are null
        }

        VMThread(HeapMemory area) {
            super(area); // special super constructor call
	    setPriorityInternal(RealtimeJavaDispatcher.SYSTEM_RT_PRIORITY);
            // all parameters are null
        }

    }


    /**
     * Watchdog timer for deadline monitoring.
     * <p>This is created and started in start() if there is a deadline
     * miss handler set. If there is no handler but this is a periodic
     * thread then it is created anyway, in case a handler is set while
     * we're waiting for the next period. By creating at start time we should
     * avoid any memory area compatibility issues.
     */
    DeadlineMonitor watchdog;

    /**
     * Allows access for the real-time dispatcher to clear our interrupt
     * flag directly.
     */
    void clearInterrupt() {
        LibraryImports.threadSetInterrupted(this, false);
    }

    /* static methods */

    /** 
     * Gets a reference to the current instance of RealtimeThread.
     * @return a reference to the current instance of RealtimeThread.
     * @throws ClassCastException If the current thread is not a
     * <code>RealtimeThread</code>.
     */
    public static RealtimeThread currentRealtimeThread() {
        return (RealtimeThread) Thread.currentThread();
    }

    /**
     * Gets the current memory area of the currently running thread.  Note that
     * this works from anywhere, including regular vanilla Java threads.
     * @return A reference to the current {@link MemoryArea} object.
     */
    public static MemoryArea getCurrentMemoryArea() {
        return MemoryArea.getMemoryAreaObject(LibraryImports.getCurrentArea());
    }


    /**
     * Returns the position in the initial memory area stack, of the initial
     * memory area for the current real-time thread. Memory area stacks may
     * include inherited stacks from parent threads. The initial memory area of
     * a <tt>RealtimeThread</tt> or <tt>AsyncEventHandler</tt> is the memory 
     * area given as a parameter to its constructor. The index in the initial 
     * memory area stack of the initial memory area is a fixed property of the 
     * real-time thread.
     *
     * <p>If the current memory area stack of the current real-time thread is
     * not the original stack and the memory area at the initial memory area
     * index is not the initial memory area, then IllegalStateException is
     * thrown.
     *
     * @return The index into the initial memory area stack of the initial
     * memory area of the current <tt>RealtimeThread</tt>
     *
     * @throws IllegalStateException if the memory area at the initial memory
     * area index, in the current scope stack is not the initial memory area.
     *
     * @throws ClassCastException if the current execution context is that of
     * a Java thread.
     *
     * @spec RTSJ 1.0.1(b) clarification XXX
     */
    public static int getInitialMemoryAreaIndex() {
        RealtimeThread current = currentRealtimeThread();
        if (current.initAreaIndex >= current.getScopeStack().size ||
            current.getScopeStack().stack[current.initAreaIndex] != 
            current.initArea.area)
            throw new IllegalStateException("Initial memory area is not in current scope stack at initial index");
        return current.initAreaIndex;
    }


    /**
     * Gets the size of the stack of {@link MemoryArea} instances to which 
     * this <code>RealtimeThread</code> has access.
     * @return The size of the stack of {@link MemoryArea} instances.
     */
    public static int getMemoryAreaStackDepth() {
        return currentRealtimeThread().getScopeStack().size;
    }

    /**
     * Gets the instance of {@link MemoryArea} in the memory area stack at 
     * the index given. If the given index does not exist in the memory area 
     * scope stack then null is returned.
     * @param index The offset into the memory area stack.
     * @return The instance of {@link MemoryArea} at index or 
     * <code>null</code> if the given value does not correspond to a position 
     * in the stack.
     */
    public static MemoryArea getOuterMemoryArea(int index) {
            return currentRealtimeThread().getScopeStack().areaAt(index);
    }

    /**
     * An accurate timer with nanosecond granularity. 
     * The actual resolution available for the clock must be queried from 
     * somewhere else. The time base is the given {@link Clock}. 
     * The sleep time may be relative or absolute. 
     * If relative, then the calling thread is blocked for the amount of time 
     * given by the parameter. 
     * If absolute, then the calling thread is blocked until the indicated
     * point in time. If the given absolute time is before the current time, 
     * the call to sleep returns immediately.
     * 
     * @param clock The instance of {@link Clock} used as the base.
     * @param time The amount of time to sleep or the point in time at which 
     * to awaken.
     * @throws InterruptedException If interrupted.
     */
    public static void sleep(Clock clock, HighResolutionTime time)
        throws InterruptedException {
        // I have no idea how you are supposed to use the Clock value
        // ??? Somehow you would have to map the given clock to the RTC ???
        // OR have a sleeping mechanism associated with the specified clock
        if (time instanceof AbsoluteTime) {
            rtd.sleepAbsolute(time.toNanos());
        }
        else {
            Thread.sleep(time.getMilliseconds(), time.getNanoseconds());
        }
    }

    /**
     * An accurate timer with nanosecond granularity. 
     * The actual resolution available for the clock must be queried from 
     * somewhere else. The time base is the default {@link Clock}. 
     * The sleep time may be relative or absolute. 
     * If relative, then the calling thread is blocked for the amount of time 
     * given by the parameter. 
     * If absolute, then the calling thread is blocked until the indicated
     * point in time. If the given absolute time is before the current time, 
     * the call to sleep returns immediately.
     * 
     * @param time The amount of time to sleep or the point in time at which 
     * to awaken.
     * @throws InterruptedException If interrupted.
     */
    public static void sleep(HighResolutionTime time)
        throws InterruptedException {
        sleep(Clock.getRealtimeClock(), time);
    }


    /* constructors */


    /**
     * package-private constructor that bypasses use of parameter objects.
     * Sets the initial MA to be the passed in heap area (ie the heap)
     */
    RealtimeThread(HeapMemory area) {
        super();
        scopeStack = new ScopeStack(area);
        initArea = area;
        initAreaIndex = 0;
        if (ASSERTS_ON) 
            Assert.check(scopeStack.stack[initAreaIndex] == initArea.area ? 
                         Assert.OK : "got the initial area index wrong");
    }

    /**
     * package-private constructor that bypasses use of parameter objects.
     * The argument is a dummy just to produce a new overloaded form.
     * NOTE: this doesn't set the initial memory area correctly so in the
     * future we'll take just the MA as a parameter.
     * @param notUsed
     */
    RealtimeThread(int notUsed) {
        super();
        // just inherit current stack
        scopeStack = ScopeStack.copyCurrentStack();
        // FIXME: this might not be right
        initArea = scopeStack.getCurrentArea();
        initAreaIndex = scopeStack.size-1;
        if (ASSERTS_ON && false) 
            Assert.check(scopeStack.stack[initAreaIndex] == initArea.area ? 
                         Assert.OK : "got the initial area index wrong");
    }


    /**
     * package-private constructor that bypasses use of parameter objects.
     * NOTE: this doesn't set the initial memory area correctly
     * @param logic the run method for this thread
     */
    RealtimeThread(Runnable logic) {
        super(logic);
        // just inherit current stack
        scopeStack = ScopeStack.copyCurrentStack();
        // FIXME: this might not be right
        initArea = scopeStack.getCurrentArea();
        initAreaIndex = scopeStack.size-1;
        if (ASSERTS_ON && false) 
            Assert.check(scopeStack.stack[initAreaIndex] == initArea.area ? 
                         Assert.OK : "got the initial area index wrong");

    }

    /** 
     * Create a real-time thread. All parameter values are <code>null</code>.
     */
    public RealtimeThread() {
        this(null, null, null, null, null, null);
    }


    /** 
     * Create a real-time thread with the given {@link SchedulingParameters}.
     * @param scheduling The {@link SchedulingParameters} associated with 
     * <code>this</code> (and possibly other instances of {@link Schedulable}).
     */
    public RealtimeThread(SchedulingParameters scheduling) {
        this(scheduling, null, null, null, null, null);
    }

    /** 
     * Create a real-time thread with the given {@link SchedulingParameters} 
     * and {@link ReleaseParameters}.
     * @param scheduling The {@link SchedulingParameters} associated with
     * <code>this</code> (and possibly other instances of {@link Schedulable}).
     * @param release The {@link ReleaseParameters} associated with 
     * <code>this</code> (and possibly other instances of {@link Schedulable}).
     */
    public RealtimeThread(SchedulingParameters scheduling,
                          ReleaseParameters release) {
        this(scheduling, release, null, null, null, null);
    }

    /** 
     * Create a real-time thread with the given characteristics and a
     * {@link Runnable}.
     * @param scheduling The {@link SchedulingParameters} associated with 
     * <code>this</code> (and possibly other instances of {@link Schedulable}).
     * @param release The {@link ReleaseParameters} associated with 
     * <code>this</code> (and possibly other instances of {@link Schedulable}).
     * @param memory The {@link MemoryParameters} associated with 
     * <code>this</code> (and possibly other instances of {@link Schedulable}).
     * @param area The {@link MemoryArea} associated with 
     * <code>this</code> (and possibly other instances of {@link Schedulable}).
     * @param group The {@link ProcessingGroupParameters} associated with 
     * <code>this</code> (and possibly other instances of {@link Schedulable}).
     */
    public RealtimeThread(SchedulingParameters scheduling,
                          ReleaseParameters release, 
                          MemoryParameters memory,
                          MemoryArea area, 
                          ProcessingGroupParameters group,
                          Runnable logic)
        throws ScopedCycleException {
        super(logic);

	scheduler = (PriorityScheduler) Scheduler.getDefaultScheduler();
//          System.out.println("scheduler set to " + scheduler);
        if (scheduling == null) {
            scheduling = scheduler.getDefaultSchedulingParameters(); // this method is not defined in the RTSJ
        }
        if (ASSERTS_ON) 
            Assert.check(scheduling != null ? Assert.OK : 
                         "Can't have null scheduling parameters");
        setSchedulingParameters(scheduling);

        if (release == null) {
            release = scheduler.getDefaultReleaseParameters(); // this method is not defined in the RTSJ
        }
        setReleaseParametersInternal(release,true);

        if (memory == null) {
            memory = scheduler.getDefaultMemoryParameters(); // this method is not defined in the RTSJ
        }
        setMemoryParameters(memory);

        if (group == null) {
            group = scheduler.getDefaultProcessingGroupParameters(); // this method is not defined in the RTSJ
        }
        setProcessingGroupParameters(group);


        scopeStack = ScopeStack.copyCurrentStack();
        if (area == null) {
            area = scopeStack.getCurrentArea();
        }
       
        initArea = area;

        // if our init-area is not the current area then we need to "push it"
        // on the scope stack - this may fail if the single-parent rule isn't
        // met. Note that if a created thread
        // is never started, and so never terminates, then in this case the
        // initArea can never be reclaimed. This isn't a bug, just a fact.
        if (initArea != scopeStack.getCurrentArea()) {
            initArea.upRefForEnter(this);
            scopeStack.push(initArea);
            didIMAPushOnConstruct = true;
        }

        initAreaIndex = scopeStack.size-1;
        if (ASSERTS_ON && false) 
            Assert.check(scopeStack.stack[initAreaIndex] == initArea.area ? 
                         Assert.OK : "got the initial area index wrong");
    }

    /* Overridden methods */
    
    /**
     * @throws ScopedCycleException
     */
    public void start() {
        // need to make the MA of 'this' the current MA while we allocate
        // all the associated helper objects.
        // Note: we have a problem with exceptions. If they are created in
        // 'this' area and propagate out then someone can catch an exception
        // from a short-lived scope. We deal with IllegalThreadStateException
        // ourselves - no problem. OOME is also no problem. Anything else is
        // a bug - not sure what the consequences will be.

        Opaque thisArea = LibraryImports.areaOf(this);
        Opaque current = LibraryImports.getCurrentArea();
        LibraryImports.setCurrentArea(thisArea);
        try {
            synchronized(rtLock) {
                // we need to ensure we only get called once per thread and
                // we can't hold the lock while invoking super.start() so we
                // use the start time as a flag
                if (startTimeNanos != 0) {
                    // we can do this in the right place - the extra set in
                    // the finally clause doesn't hurt
                    LibraryImports.setCurrentArea(current);
                    throw new IllegalThreadStateException("can't start a thread more than once");
                }
                
                // now check to see if we need to set up a watchdog timer for
                // deadline monitoring: we do for periodics, otherwise only if
                // the deadline miss handler is set.
                
                boolean needWatchDog = false;
                AsyncEventHandler deadlineMissHandler = null;
                
                if (rParams != null) {
                    deadlineMissHandler = rParams.getDeadlineMissHandler();
                    needWatchDog = (deadlineMissHandler != null);
                    if (DEBUG_DEADLINES) {
                        System.out.println(getName() +
                                           (needWatchDog ? 
                                            " watchdog needed due to miss handler" :
                                            " watchdog not needed as no miss handler"));
                    }
                }
                
                HighResolutionTime start = null;
                if (rParams instanceof PeriodicParameters) {
                    if (DEBUG_DEADLINES) {
                      System.out.println("Release parameters are periodic.");
                    }
                    needWatchDog = true; // always create for periodic threads
                    if (DEBUG_DEADLINES) {
                        System.out.println(getName() + 
                                           " Need watchdog for periodic thread");
                    }
                    PeriodicParameters pp = (PeriodicParameters)rParams;
                    start = pp.getStart();
                    periodNanos = pp.getPeriodNanos();
                    if (DEBUG_DEADLINES) {
                        System.out.println("Starting thread " + getName() +
                                           "\n - start time = " + start.toNanos() +
                                           "\n -     period = " + periodNanos +
                                           "\n -   deadline = " + pp.getDeadlineNanos() );
                    }
                }
                
                AbsoluteTime now = Clock.getRealtimeClock().getTime();
                
                if (start instanceof AbsoluteTime) {
                    if (DEBUG_DEADLINES) {
                      System.out.println("Start parameter is absolute.");
                    }                
                    if (start.compareTo(now) < 0) {
                        start = now;
                    }
                }
                else if (start instanceof RelativeTime){ 
                    if (DEBUG_DEADLINES) {
                      System.out.println("Start parameter is relative.");
                    }                  
                    //System.out.println("Adding relative start of " + start.toNanos());
                    start = now.add((RelativeTime)start);
                }
                else {
                    if (DEBUG_DEADLINES) {
                      System.out.println("Start parameter is neither absolute, nor relative.");
                    }                
                    start = now;
                }
                // start is now an absolute time
                startTimeNanos = start.toNanos();
                startPeriodFrame = startTimeNanos;
                //System.out.println(getName() + " Latched start time " + startTimeNanos);

                // we've now marked this thread as started. If we get an
                // out-of-memory-exception in the following then we need to
                // unmark this thread so that the start() can be retried once
                // the memory condition is fixed.


                try {
                    // create and start the watchdog
                    if (needWatchDog) {
                        watchdog = new DeadlineMonitor(
                                                       ((AbsoluteTime)start).add(rParams.getDeadline()), 
                                                       Clock.getRealtimeClock(),
                                                       deadlineMissHandler);
                        watchdog.start();
                        if (DEBUG_DEADLINES) {
                            System.out.println(getName() + 
                                               " watchdog started and enabled");
                        }
                    }
                }
                catch (OutOfMemoryError oome) {
                    startTimeNanos = 0; //restore to unstarted state
                    throw oome;
                }
            }
        
            // need to increment the ref count of our initArea if it wasn't
            // pushed at construction time
            if (!didIMAPushOnConstruct && initArea instanceof ScopedMemory)
                ((ScopedMemory)initArea).upRef();
            
            try {
                // release lock before calling super.start
                // Note: this can't throw an exception other than OOME or an
                // internal error of some kind. The former is a not a problem
                // because it's allocated in immortal. For internal errors we
                // don't try to do anything.
                super.start();
            }
            catch (OutOfMemoryError oome) {
                try {
                    startTimeNanos = 0; //restore to unstarted state
                    // NOTE: it is possible that the watchdog could try and 
                    // fire before we get here.
                    if (watchdog != null) {
                        watchdog.destroy();
                        watchdog = null; // this leaks in immortal or scoped
                    }
                    // need to decrement ref count of the initArea 
                    // if we upped it
                    if (!didIMAPushOnConstruct && initArea instanceof ScopedMemory)
                        ((ScopedMemory)initArea).downRef();
                }
                finally {
                    if (true)  // avoid annoying javac warning
                        throw oome;
                }
            }
        }
        finally {
            LibraryImports.setCurrentArea(current);
        }

    }

    /**
     * Invoke the appropriate method on the dispatcher to start this thread.
     * This needs to be overridden by RealtimeThread to allow for start times.
     * <p>FIX ME: we can't have protected methods :(
     */
    protected void invokeStartThread() {
        rtd.startThreadDelayed(this, startTimeNanos);
    }
    

    /**
     * Overrides {@link Thread#preRun} to check basic scope stack and start 
     * time
     * <p><b>NOTE:</b> When this runs, the VM has already established the
     * initial memory area as the current allocation context before
     * <tt>Thread.runThread</tt> was invoked by the ED. Hence any exceptions
     * that propagate out can be handled by <tt>runThread</tt> without concern
     * for which allocation context they were created in.
     */
    void preRun() {
        if (ASSERTS_ON) {
            Assert.check(scopeStack != null ? Assert.OK : "null  scope stack");
            Assert.check(scopeStack.getCurrentArea() == getCurrentMemoryArea() ? 
                         Assert.OK : 
                         "initial MA was not set up correctly before runThread");
        }

        if (DEBUG_SCOPESTACK) scopeStack.dump("start of runThread()");

        /*
          this assertion does not hold for threads with start scheduled in the future (release parameter)
          such threads are however ok
          
        if (ASSERTS_ON || DEBUG_DEADLINES) {
            long now = Clock.RealtimeClock.getCurrentTimeNanos();
            Assert.check( now >= startTimeNanos ? Assert.OK :  
                          Thread.currentThread() + 
                          " execution time "+ now +  
                          " is before specified start time " + startTimeNanos);
            if (DEBUG_DEADLINES) {
                System.out.println(getName() + 
                                   " scheduled start at " + startTimeNanos +
                                   "\n            actual start at " + now);
            }
        }
        */
        if (DEBUG_DEADLINES) {
            long now = Clock.RealtimeClock.getCurrentTimeNanos();

            System.out.println(getName() + 
                                   " scheduled start at " + startTimeNanos +
                                   "\n            actual start at " + now);
        }        
    }

    /** flag to indicate whether we have to defer scope stack cleanup
        until finalization time. This is necessary if we have to hand-off
        to the scope finalizer thread due to a child scope of the IMA being 
        processed by it
    */
    boolean doScopeCleanUpInFinalizer = false;

    /**
     * Overrides {@link Thread#postRun} to cleanup the deadline watchdog,
     * tear down scope stacks etc
     */
    void postRun() {
        // need to lock the thread to ensure noone tries to play with
        // parameters etc while we tear down.
        synchronized(rtLock) {
            terminating = true;

            // clean-up the watch dog
            if (watchdog != null) {
                watchdog.disable();
                if (DEBUG_DEADLINES) {
                    System.out.println(getName() + 
                                       " watchdog disabled: about to destroy");
                }
                watchdog.destroy();
            }


            // At this stage of termination a no-heap stops acting as a no-heap
            // thread as cleanup of the scope stack can encounter a heap 
            // allocated scope. In one sense changing the nature of the current
            // thread is not an issue because it is terminating and if it
            // happens to get delayed by the GC then so what. The only glitch
            // is that finalizers execute application code and this 
            // inconsistency
            // between the type of thread (NHRT) and its actual behaviour can
            // be seen by the finalizer. We can live with this because it is
            // less of a problem that any other solution.

            if (this instanceof NoHeapRealtimeThread)
                LibraryImports.disableHeapChecksForTermination(this);

            // Clean-up scope stack. We downRef our initial MA which could
            // cause its parent to be cleared and thus for the parent to no
            // longer have children. If the parent has no child and a ref count
            // of zero then we clear its parent too and so forth. This is a 
            // two phase protocol to ensure that scopes don't get cleared while
            // we're still using them. In the first phase the scope is locked
            // and a normal downRef style sequence applied. In the second phase
            // just prior to actual thread termination we release the scope
            // locks. This prevents another thread from reentering the scope
            // causing it to be cleared while this thread - which may have
            // been allocated in the scope (or its parent) - is still being
            // referenced by the system.

            if (initArea instanceof ScopedMemory) {
                if (DEBUG_SCOPESTACK) {
                    System.out.print("doing startDownRefForTermination in");
                    System.out.println(initArea);
                }
                // startDownRef returns true if it did and false if it could
                // not because the scope finalizer thread is involved
                doScopeCleanUpInFinalizer = 
                     ! ((ScopedMemory)initArea).startDownRefForTermination();
            }


        } // release rtlock
    }    

    // NOTE: when this runs  our vmThread reference has already been cleared
    private final void finalizeThread() {
        String me = null;
        if (DEBUG_SCOPESTACK) {
            me = getName();
            System.out.print(me);
            System.out.println(" finalizeThread is running");
        }

        if (doScopeCleanUpInFinalizer) {
            ((ScopedMemory)initArea).doIMACleanup();
            // we could in theory be 'blown away' at any time now so
            // no references to 'this' allowed from this point
            return;
        }


        if (initArea instanceof ScopedMemory) {
            if (DEBUG_SCOPESTACK) {
                System.out.print(" doing finishDownRefForTermination in");
                System.out.println(initArea);
            }
            // walkthrough the scope-stack and finish downRef'ing each scope
            // we start with what should be the initial MA
            if (ASSERTS_ON && false) 
                Assert.check(scopeStack.areaAt(scopeStack.getDepth()-1)==initArea ?
                             Assert.OK : "Hmmm initArea is not where we thought");
            for (int i = scopeStack.getDepth()-1; i >=0 ; i--) {
                MemoryArea area = scopeStack.areaAt(i);
                if (area instanceof ScopedMemory) 
                    ((ScopedMemory)area).finishDownRefForTermination();
            }
        }

        scopeStack.free();  // release our scope stack

        if (DEBUG_SCOPESTACK) {
            System.out.print(me);
            System.out.println(" finalizeThread complete");
        }
    }



    /* RealtimeThread specific methods */
    
    /**
     * Stop unblocking {@link #waitForNextPeriod} for <code>this</code> 
     * if the type of the associated instance of {@link ReleaseParameters} 
     * is {@link PeriodicParameters}.
     * If that does not have a type of {@link PeriodicParameters} , 
     * nothing happens.
     */
    public void deschedulePeriodic() {
        // we don't need to inspect the release parameters as only wfnp
        // interacts with this value, and it does check for periodic params
        synchronized(rtLock) {
            schedulePeriodic = false;
        }
    }

    /**
     * Begin unblocking {@link #waitForNextPeriod} for a periodic thread. 
     * Typically used when a periodic schedulable object is in an overrun 
     * condition. The scheduler should recompute the schedule and perform 
     * admission control.
     * If <code>this</code> does not have a type of {@link PeriodicParameters}
     * as its {@link ReleaseParameters} nothing happens.
     */
    public void schedulePeriodic() {
        synchronized(rtLock) {
            schedulePeriodic = true;
            rtLock.notify();
        }
    }

    /**
     * Return the initial memory area for this <tt>RealtimeThread</tt>
     * (corresponding to the <tt>area</tt> parameter for the constructor.
     * <p><em>Note:</em> Unlike the scheduling-related parameter objects, 
     * there is never a case where a default parameter will be constructed for 
     * the thread. The default is a <em>reference</em> to the current 
     * allocation context when <tt>this</tt> is constructed.
     *
     * @return A reference to the initial memory area for this thread.
     *
     * @spec RTSJ 1.1
     */
    public MemoryArea getMemoryArea() {
        return initArea;
    }

    /**
     * Sets the state of the generic {@link AsynchronouslyInterruptedException}
     * to pending.
     */
    public void interrupt() {
        // ### TODO: need to hook into AIE
        super.interrupt();
    }

    /* methods defined in Schedulable */

    public boolean addIfFeasible() {
        synchronized(rtLock) {
            if (scheduler != null) {
                synchronized(scheduler.lock) {
                    scheduler.addToFeasibility(this);
                    if (!scheduler.isFeasible()) {
                        scheduler.removeFromFeasibility(this);
                        return false;
                    }
                    return true;
                }
            }
            return false;
        }
    }

    public boolean addToFeasibility() {
        synchronized(rtLock) {
            if (scheduler != null) {
                return scheduler.addToFeasibility(this);
            }
            else {
                return false;
            }
        }
    }

    public MemoryParameters getMemoryParameters() {
        return mParams;
    }

    public ProcessingGroupParameters getProcessingGroupParameters() {
        return gParams;
    }

    public ReleaseParameters getReleaseParameters() {
        return rParams;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public SchedulingParameters getSchedulingParameters() {
        // this lock is necessary to ensure we don't expose temporary
        // parameters when setSchedulingParameters is about to fail
        synchronized(rtLock) {
            return sParams;
        }
    }

    public boolean removeFromFeasibility() {
        synchronized(rtLock) {
            if (scheduler != null) {
                return scheduler.removeFromFeasibility(this);
            }
            else {
                return false;
            }
        }
    }

    public boolean setIfFeasible(ReleaseParameters release,
                                 MemoryParameters memory) {
	return setIfFeasible(release, memory, gParams);
    }

    public boolean setIfFeasible(ReleaseParameters release,
                                 MemoryParameters memory,
                                 ProcessingGroupParameters group) {
        synchronized(rtLock) {
            if (scheduler != null) {
                return scheduler.setIfFeasible(this, release, memory, group);
            }
            else {
                return false;
            }
        }
    }

    public boolean setIfFeasible(ReleaseParameters release,
                                 ProcessingGroupParameters group) {
	return setIfFeasible(release, mParams, group);
    }

    public void setMemoryParameters(MemoryParameters parameters) {
        synchronized(rtLock) {
            if (mParams != null) {
                mParams.deregister(this);
            }
            mParams = parameters;
            if(mParams != null) {
                mParams.register(this);
            }
        }
    }

    public boolean setMemoryParametersIfFeasible(MemoryParameters memory) {
        if (memory == null) {
            return false;
        }
        else {
            return setIfFeasible(rParams, memory, gParams);
        }
    }

    public void setProcessingGroupParameters(ProcessingGroupParameters parameters) {
        synchronized(rtLock) {
            if(gParams != null) {
                gParams.deregister(this);
            }
            gParams = parameters;
            if(gParams != null) {
                gParams.register(this);
            }
        }
    }

    public boolean setProcessingGroupParametersIfFeasible(ProcessingGroupParameters group) {
        if (group == null) {
            return false;
        }
        else {
            return setIfFeasible(rParams, mParams, group);
        }
    }

    public void setReleaseParameters(ReleaseParameters release) {
        setReleaseParametersInternal(release, false);
    }

    /**
     * Internal method for setting the release parameters. We need to 
     * distinguish between setting at construction time and at other
     * times - or at least we thought we did, but now it doesn't seem
     * necessary. We keep this structure though as the semantics for this
     * stuff is in a state of flux.
     * <p>The dynamic behaviour of changing releaase parameters is rather
     * vague. The spec will say that such changes need not take affect until
     * the next release - but that in itself is not a well-defined statement.
     * Note that changing the parameters before the thread is started is
     * simple and the starting mechanism will take care of everything.
     *
     * <p>If we changed periodic params whilst holding periodic params the
     * behaviour of wfnp is as follows.
     * If the current release occurred at time S+iT, where S is the period
     * frame start, and T is the period associated with that period frame, 
     * then the next release will occur at time S+(i+1)T, the period frame is
     * changed to S' = S+(i+1)T and the period T' is read from the periodic
     * parameters. The next release thus occurs at time S'+0T', and thereafter 
     * each release occurs at a time S'+iT' for i = 1, 2, 3 ...
     * <p>If we change to having periodic parameters when we did not have
     * periodic parameters, then things are more complex - when should the next
     * release from wfnp occur? The TIC has decided that the behaviour should 
     * be as follows: on waitForNextperiod the next release time will be set
     * to a time t = S+nT for the smallest n such that t > now
     *
     * @param release the release parameters object
     * @param constructing if <tt>true</tt> indicates this method was
     * called as part of the constructor sequence.
     * @see #waitForNextPeriod
     */
    void setReleaseParametersInternal(ReleaseParameters release,
                                      boolean constructing) {
        synchronized(rtLock) {
            boolean wasPeriodic = rParams instanceof PeriodicParameters;
            if(rParams != null) {
                rParams.deregister(this);
            }
            rParams = release;
            if(rParams != null) {
                rParams.register(this);
            }

            // nothing else to do on construction
            if (constructing) {
                return;
            }

            // any change to periodicParameters is handled by wfnp
            // we just tidy up if we're no longer periodic
            if (!(rParams instanceof PeriodicParameters) &&
                wasPeriodic ) {
                // clear periodNanos
                periodNanos = -1;

                // we're still in a release and could still miss the
                // deadline associated with that release. Only problem
                // is we are no longer going to call wfnp so what constitutes
                // our completion ??? Presumably we will terminate, but if
                // not then the watchdog will go off - which seems reasonable.
            }
        }
    }

    public boolean setReleaseParametersIfFeasible(ReleaseParameters release) {
        if (release == null) {
            return false;
        }
        else {
            return setIfFeasible(release, mParams, gParams);
        }
    }

    // there is only one scheduler and I'm not permitting null.
    public void setScheduler(Scheduler scheduler) {
        if (ASSERTS_ON) 
            Assert.check(scheduler == this.scheduler ? 
                         Assert.OK : "wrong scheduler");
    }

    public void setScheduler(Scheduler scheduler,
                             SchedulingParameters scheduling,
                             ReleaseParameters release, 
                             MemoryParameters memory,
                             ProcessingGroupParameters group) {
        synchronized(rtLock) { 
            if (scheduler != null) {
                setScheduler(scheduler);
            }
            if (scheduling != null) {
                setSchedulingParameters(scheduling);
            }
            if (release != null) {
                setReleaseParameters(release);
            }
            if (memory != null) {
                setMemoryParameters(memory);
            }
            if (group != null) {
                setProcessingGroupParameters(group);
            }
        }
    }

    /**
     * @throws IllegalArgumentException if the parameter is <code>null</code>.
     * The RTSJ doesn't define this but it makes no sense to have a RT thread
     * without any scheduling parameters. (It also makes no sense to not have
     * a scheduler but the RTSJ allows it.)
     */
    public void setSchedulingParameters(SchedulingParameters s) {
        // note that changing the priority of the scheduling parameters causes
        // each schedulable to 'replace' its params with the modified version.
        // This is a very heavyweight and expensive operation for existing
        // schedulables - assuming they are in a state that will allow this
        // change. Best practice is to create the scheduling params and pass
        // them to the thread constructor and never change the priority
        // again.

        if (s == null) {
            throw new IllegalArgumentException(
                "null scheduling parameters not allowed");
        }

        // Q: what if p is not of type PriorityParameters and we are bound to
        // the PriorityScheduler? Can't happen in OVM right now of course.
        if (ASSERTS_ON) 
            Assert.check(s instanceof PriorityParameters ? Assert.OK :
                         "can't handle scheduling parameters of given type");

        PriorityParameters p = (PriorityParameters) s;
        int prio = p.getPriority();
        if (!(s instanceof org.ovmj.java.UncheckedPriorityParameters)
	    && !scheduler.isValid(prio)) {
            throw new IllegalArgumentException("priority " + prio + 
                                               " out of range");
        }

        // need to protect access to this. Note that this thread
        // can not terminate while we are doing this.
        synchronized(rtLock) {
            // this is a small window between a terminating thread releasing
            // its lock and isAlive returning false. We use a second flag to
            // ensure this window is closed.
            if (isAlive() && !terminating) {
                // if we succeed in changing the priority then we must have
                // the new parameters registered. Note
                // that the locking prevents anyone from seeing this
                // change until we want them to. We also rely on a change to
                // the original parameters object being implemented by calling
                // this method with the same parameter object - so our old
                // parameters can't change underneath us either.

                // Ideally we would not hold the lock
                // when doing the call, as that can cause priority inversion,
                // but without the lock we could see incorrect parameters and
                // we wouldn't have atomicity, and the thread could terminate
                // etc.

                // try to make the change
                if (!rtd.canSetSchedulingParameters(this, prio)) {
                    throw new IllegalThreadStateException(
                        "can't change scheduling parameters when thread " +
                        "is alive but not in a monitor wait or sleep"
                    );
                }
            }
            // change is allowed
            if (sParams != null) { // could be called from constructor
                sParams.deregister(this);
            }
            sParams = p;
            sParams.register(this);

            setPriorityInternal(prio);
        }
    }

    public boolean setSchedulingParametersIfFeasible(
        SchedulingParameters scheduling)  {
	if (scheduling == null || scheduler == null) {
	    return false;
        }

        synchronized(rtLock) {
            synchronized(scheduler.lock) {
		SchedulingParameters temp = sParams;
		removeFromFeasibility();
                try {
                    setSchedulingParameters(scheduling);
                }
                catch(IllegalThreadStateException ex) {
                    return false;
                }
		if (!addIfFeasible()) {
                    setSchedulingParameters(temp);
                    addToFeasibility();
                    return false;
                }
	    }
        }
	return true;
    }


    /** Special debug flag during testing */
    // make non-static and non-final if you want to turn on per-thread
    public static final boolean DEBUG_WFNP = false;

    /**
     * Used by threads that have a reference to a {@link ReleaseParameters}
     * type of {@link PeriodicParameters} to block until the start of each 
     * period. 
     * Periods start at either the start time in {@link PeriodicParameters}
     * or when <code>this.start()</code> is called. 
     * This method will block until the start of the next period unless the 
     * thread is in either an overrun or deadline miss condition.
     * If both overrun and miss handlers are <code>null</code> and the thread 
     * has overrun its cost or missed a deadline 
     * <code>waitForNextPeriod()</code> will immediately return 
     * <code>false</code> once per overrun or deadline miss. 
     * It will then again block until the start of the next period (unless, 
     * of course, the thread has overrun or missed again). 
     * If either the overrun or deadline miss handlers are not 
     * <code>null</code> and the thread is in either an overrun or deadline 
     * miss condition <code>waitForNextPeriod()</code> will block until the 
     * handler corrects the situation (possibly by calling 
     * {@link #schedulePeriodic}).
     *
     * @return <code>true</code> when the thread is not in an overrun or 
     * deadline miss condition and unblocks at the start of the next period.
     *
     * @throws IllegalThreadStateException If <code>this</code> does not
     * have a reference to a {@link ReleaseParameters} type of
     * {@link PeriodicParameters}.
     * @throws IllegalThreadStateException If the current thread is not
     * <code>this</code> (Implementation specific exception)
     */
    public static boolean waitForNextPeriod() throws IllegalThreadStateException {
      return currentRealtimeThread().waitForNextPeriodInternal();
    }
    
    private boolean waitForNextPeriodInternal() throws IllegalThreadStateException {
        ReleaseParameters release_temp = rParams; // snap-shot incase it changes
        if (! (release_temp instanceof PeriodicParameters)) {
            throw new IllegalThreadStateException("wrong ReleaseParameter type");
        }
        PeriodicParameters release = (PeriodicParameters) release_temp;

        if (RealtimeThread.currentRealtimeThread() != this) {
            throw new IllegalThreadStateException(
                "waitForNextPeriod not called by the current thread");
        }

        // we will block here under two circumstances:
        // 1. Application code has called deschedulePeriodic
        // 2. we missed a deadline and we have a miss-handler installed
        //
        // In the first case we should not disable the watchdog.
        // In the second case the watchdog has already fired so we don't need
        // to disable it.

        boolean interrupted = false;
        if (DEBUG_WFNP) System.out.println(this.getName() + " acquiring rtLock");
        synchronized(rtLock) {
            if (DEBUG_WFNP) System.out.println(this.getName() + " acquired rtLock");
            while (!schedulePeriodic) {
                if (DEBUG_WFNP) System.out.println(this.getName() + " waiting for schedulePeriodic");
                try {
                    rtLock.wait();
                }
                catch(InterruptedException ie) {
                    if (DEBUG_WFNP) System.out.println(this.getName() + " interrupted while waiting");
                    interrupted = true;
                }
            }
            if (DEBUG_WFNP) System.out.println(this.getName() + " schedulePeruiodic ok");
        }
        if (DEBUG_WFNP) System.out.println(this.getName() + " released rtLock");

        // re-assert the interrupt if needed. We use an uninterruptible sleep
        // later on so that's okay.
        if (interrupted) {
            // we can just set the interrupt field directly because we
            // know we aren't in an interuptible method.
            LibraryImports.threadSetInterrupted(this, true);
        }

        // If we had to wait our parameter type and/or values could have 
        // changed while we were waiting. 
        if (release != rParams) {
            if (DEBUG_WFNP) System.out.println(this.getName() + " release parameters changed whilst waiting");
            release_temp = rParams;
            if (! (release_temp instanceof PeriodicParameters)) {
                throw new IllegalThreadStateException(
                    "wrong ReleaseParameter type");
            }
            release = (PeriodicParameters) release_temp;
        }

        // given a period frame start time S and a period T then the release 
        // times are S, S+1T, S+2T, ...
        // If the period is changed we don't recognise that until we reach
        // this point.

        long pNanos = -1;
        long dNanos = -1;
        AsyncEventHandler overrun = null;
        AsyncEventHandler miss = null;
        synchronized(release) {
            pNanos = release.getPeriodNanos();
            dNanos = release.getDeadlineNanos();
            overrun = release.getCostOverrunHandler();
            miss = release.getDeadlineMissHandler();
        }

        if (DEBUG_WFNP) {
            System.out.println("Period = " + pNanos);
            System.out.println("Deadline = " + dNanos);
        }

        // calculate the next wake up time (watchdog may still be ticking)
        long now = Clock.RealtimeClock.getCurrentTimeNanos();

        if (DEBUG_WFNP) {
            System.out.println("Current time is: " + now);
        }
        long interval = now - startPeriodFrame;
        long nPeriods = 0;
        long nextPeriodNanos = 0;

        // this may be the first time we've called wfnp due to recently
        // becoming as periodic thread. periodNanos tells us if this is
        // the case. To calculate the next release time we find a time t in
        // the future where t=S+nT for the first n that makes t in the future.
        // ie. we act as if this thread had had the given period since it
        // was started
        if (periodNanos == -1) {
            if (DEBUG_WFNP) {
                System.out.println(getName() + 
                    " WFNP: periodNanos was -1, so initializing");
            }

            periodNanos = pNanos;
            nPeriods = interval / periodNanos;
            if (nPeriods<0) { 
                // it may be still before the first release 
                // this can happen if the thread was started with start release parameter
                // nonzero
              nPeriods = -1;
            }
            nextPeriodNanos = startPeriodFrame + (nPeriods+1)*periodNanos; 

            // if we don't have a watchdog then create one
            if (watchdog == null) {
                // must create in the same MA as this
                Opaque current = LibraryImports.getCurrentArea();
                Opaque thisMA = LibraryImports.areaOf(this);
                LibraryImports.setCurrentArea(thisMA);
                try {
                    // create with a fake deadline and don't enable it
                    watchdog = new DeadlineMonitor(null, null, miss);
                    watchdog.start(true);
                }
                finally {
                    LibraryImports.setCurrentArea(current);
                }
                if (DEBUG_DEADLINES) {
                    System.out.println(getName() + 
                                       " watchdog started due to initialization in WFNP");
                }
            }
        }
        else {

            // We're still periodic but the period may have changed.
            // The next release is what it would have been without the period 
            // change. We then set the startPeriodFrame to be that next release
            // time.
            
            nPeriods = interval / periodNanos;
            if (nPeriods<0) { 
                // it may be still before the first release 
                // this can happen if the thread was started with start release parameter
                // nonzero
              nPeriods = -1;
            }            
            nextPeriodNanos = startPeriodFrame + (nPeriods+1)*periodNanos; 

            if (pNanos != periodNanos) {
                if (DEBUG_WFNP) System.out.println(this.getName() + 
                                                   " Detected period change so reseting periodstart frame");
                
                periodNanos = pNanos;
                startPeriodFrame = nextPeriodNanos;
            }

        }

        if (DEBUG_WFNP) {
            System.out.println(this.getName() + 
                               " Number of periods elapsed: " + 
                               nPeriods + "\n" +
                               "Next release time: " + nextPeriodNanos);
        }
            
        // if we miss a deadline then the deadline monitor sets deadlineMissed
        // and if we also have a handler set then the deadline monitor invokes
        // deschedulePeriodic (an overrun monitor must do similar)
        // If we missed the deadline then the watchdog will not fire again
        // so we don't need to disable it.

        if (deadlineMissed) { // || overrun occurred - if we were tracking it
            if (DEBUG_DEADLINES || DEBUG_WFNP) {
                System.out.println(getName() + " WFNP: deadline missed");
            }
            if ( overrun == null && miss == null ) {
                deadlineMissed = false; // reset flag
                // if we missed a deadline then our next deadline is what?
                // The most reasonable thing seems to be to assume that
                // we were actually released at the beginning of this
                // period and hence our deadline should be relative to that.
                // This is wrong but this is all going to change anyway - DH
                long nextDeadline = nextPeriodNanos - periodNanos + dNanos;

                if (DEBUG_DEADLINES || DEBUG_WFNP) {
                    System.out.println(getName() + 
                                        " WFNP: setting watchdog handler");
                }
                // FIXME: this will allocate :(
                watchdog.setHandler(miss); // handler might have changed

                if (DEBUG_DEADLINES || DEBUG_WFNP) {
                    System.out.println(getName() + 
                           " WFNP: resetting watchdog to " + nextDeadline);
                }
                // reset watchdog - fires immediately if deadline passed
                watchdog.reset(nextDeadline);

                if (DEBUG_DEADLINES || DEBUG_WFNP) {
                    System.out.println(getName() + 
                    " watchdog reset after deadline miss in WFNP");
                }
                return false;
            }
            else {
                // nothing: the "wait for schedule periodic" took care of this
            }
        }
        else { // haven't missed deadline
            if (DEBUG_DEADLINES || DEBUG_WFNP) {
                System.out.println(getName() + " WFNP: deadline NOT missed - stopping watchdog");
            }
            // turn off the watchdog
            watchdog.stop();

            if (DEBUG_DEADLINES || DEBUG_WFNP) {
                System.out.println(getName() + " WFNP: deadline NOT missed - watchdog stopped");
            }
        }
        deadlineMissed = false; // always reset

        if (DEBUG_DEADLINES || DEBUG_WFNP) {
            System.out.println(getName() + " WFNP: about to sleep");
        }
        if (rtd.sleepAbsoluteUninterruptible(nextPeriodNanos)) {
            if (DEBUG_DEADLINES || DEBUG_WFNP) {
                System.out.println(getName() + " WFNP: sleep returned");
            }
        }
        else {
            if (DEBUG_DEADLINES || DEBUG_WFNP) {
                System.out.println(getName() + " WFNP: sleep returned immediately");
            }
        }

        if (DEBUG_DEADLINES || DEBUG_WFNP) {
            System.out.println(getName() + " WFNP: setting watchdog handler");
        }

        watchdog.setHandler(miss); // handler might have changed

        // next deadline is deadline nanos after start of next period
        long nextDeadline = nextPeriodNanos + dNanos;

        if (DEBUG_DEADLINES || DEBUG_WFNP) {
            System.out.println(getName() + " WFNP: resetting watchdog to " 
                                + nextDeadline);
        }

        // reset watchdog - fires immediately if deadline passed
        watchdog.reset(nextDeadline);

        return true;
    }




    /**
     * Returns the amount of memory this thread has allocated in the immortal
     * memory area. In the current implementation we do not track this.
     * @return zero always
     *
     */
    /* package */ long getImmortalAllocation() {
        return 0;
    }

    /**
     * Returns the amount of memory this thread has allocated in the current
     * scoped memory area. In the current implementation we do not track this.
     * @return zero always
     *
     */
    /* package */ long getMemoryAreaAllocation() {
        return 0;
    }

    public String toString() {
        return "Realtime-" + super.toString(); 
    }


}
