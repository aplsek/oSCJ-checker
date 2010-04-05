package s3.services.java.ulv1;

import java.util.Comparator;

import ovm.core.Executive;
import ovm.core.execution.Native;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.ThreadManager;
import ovm.core.services.timer.TimerManager;
import ovm.core.stitcher.ThreadDispatchServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.core.stitcher.TimerServicesFactory;
import ovm.services.java.JavaDispatcher;
import ovm.services.java.JavaMonitor;
import ovm.services.java.JavaOVMThread;
import ovm.services.java.JavaUserLevelThreadManager;
import ovm.services.monitors.Monitor;
import ovm.services.monitors.QueryableConditionQueue;
import ovm.util.OVMError;
import s3.core.domain.S3Domain;
import s3.util.PragmaInline;
import s3.util.PragmaNoPollcheck;
import s3.util.Visitor;
import s3.util.queues.SingleLinkPriorityQueue;

/**
 * An implementation of {@link JavaMonitor} based around user-level
 * thread management with priority ordering. We also provide additional
 * query interfaces to assist in debugging and testing.
 * <p>We use a hand-off protocol such that releasing the monitor gives
 * ownership to an entering thread (if any) and makes that thread ready to
 * run. This is efficient from a scheduling perspective as waiting threads
 * don't need to run to reacquire the monitor. However, it introduces a burden
 * of communicating to a waiting thread why it woke up (timeout, interrupt or
 * signal).
 * <p>We assume thread suspension is not supported and do not check for it.
 *
 *<p>b>Note:</b> The basic monitor implementation is the same as that in 
 * <tt>s3.services.monitors.BasicMonitorImpl</tt> but we don't extend that
 * simply to keep the type forms of monitor completely separate.
 *
 * <p><B>NOTE 2:</b> The hand-off protocol is not a good thing from a real-time
 * perspective as it can lead to a lower priority thread being granted a lock
 * if a higher priority thread blocked while holding that lock (thus allowing
 * the lower priority thread to run and block trying to acquire the lock).
 * If we don't hand-off then a high priority thread can claim and release the
 * lock continuously without penalty, but the trade-off is that another thread
 * may have to be woken to see if the lock is available, only to have that 
 * thread discover it is not.
 *
 * @author David Holmes
 *
 */
public class JavaMonitorImpl extends ovm.core.OVMBase implements JavaMonitor, QueryableConditionQueue {

    /** Gather statistics on monitor operations **/
    public static boolean GATHER_MONITOR_STATISTICS = false; /// MAKE THIS FALSE IF NOT IN PROFILING MODE!
    public static int M_ENTER, M_ENTER_QUICK, M_ACQUIRE, M_INFLATE, M_RECURSIVE, M_BLOCK;
    public static int M_SWITCH;

    /** Reference to current dispatcher */
    protected static final JavaDispatcher dispatcher;

    /** Reference to current thread manager */
    protected static final JavaUserLevelThreadManager jtm;

    /** Reference to current timer manager */
    protected static final TimerManager timer;

    // This static initializer runs at image build time and expects to set
    // all the above static references from the current configuration.
    // Ideally this class would never get loaded except in a configuration
    // in which all the initialization is guaranteed to succeed, but that
    // is not yet the case. So this is written such that finding null values
    // or the wrong types of service instances, is not considered an error.
    // - DH 1 March 2005
    static {
	ThreadManager tm = ((ThreadServicesFactory) ThreadServiceConfigurator.config
		.getServiceFactory(ThreadServicesFactory.name)).getThreadManager();
	if (tm instanceof JavaUserLevelThreadManager) jtm = (JavaUserLevelThreadManager) tm;
	else jtm = null;

	OVMDispatcher disp = ((ThreadDispatchServicesFactory) ThreadServiceConfigurator.config
		.getServiceFactory(ThreadDispatchServicesFactory.name)).getThreadDispatcher();
	if (disp instanceof JavaDispatcher) dispatcher = (JavaDispatcher) disp;
	else dispatcher = null;

	timer = ((TimerServicesFactory) ThreadServiceConfigurator.config.getServiceFactory(TimerServicesFactory.name))
		.getTimerManager();

    }

    /** timer interupt granularity */
    protected static long sleepMin;

    /** The visitor used to deal with waking threads doing timed waits */
    protected static Visitor timeoutAction = new Visitor() {
	public void visit(Object t) throws PragmaNoPollcheck {
	    JavaOVMThread thread = (JavaOVMThread) t;
	    JavaMonitorImpl mon = (JavaMonitorImpl) thread.getWaitingConditionQueue();
	    // need to tell thread it timed-out so that waitAbortable can
	    // return with the right value
	    thread.setUnblockState(JavaOVMThread.UNBLOCKED_TIMED_OUT);
	    // also need to let abortWait know that this thread is no
	    // longer in the delay queue
	    thread.setState(JavaOVMThread.BLOCKED_WAIT);
	    mon.abortWait(thread);
	}
    };

    /** 
     * Performs late "static" initialization after all services have been
     * initialized and started. This is invoked via the Factory
     * initialize() method which is invoked by the dispatcher as part of
     * initializing the threading system.
     *
     */
    protected static void initialize() {
	if (timer == null || dispatcher == null || jtm == null) throw new OVMError.Configuration("null service found");
	if (!timer.isStarted()) throw new OVMError.Internal("timer not started when queried");
	sleepMin = timer.getTimerInterruptPeriod();
    }

    /** The queue of threads trying to enter this monitor */
    protected SingleLinkPriorityQueue entryQ;

    /** The queue of threads waiting upon this monitor */
    protected SingleLinkPriorityQueue waitQ;

    /** The comparator used with our queues - obtained from the thread
     *  manager if not set explicitly
     */
    protected final Comparator comp;

    /** The current owner of this monitor */
    // could be a scoped object
    protected volatile JavaOVMThread owner = null;

    /** The number of times the owner has locked this monitor */
    protected volatile int entryCount = 0;

    /** The size of the entry queue */
    protected volatile int entrySize = 0;

    /** The size of the waitQueue */
    protected volatile int waitSize = 0;

    /* constructors are not public because construction should occur via
     * a factory.
     */

    /**
     * Construct a monitor using the default comparator as configured in
     * the current thread manager.
     */
    protected JavaMonitorImpl() {
	this(jtm.getComparator());
	if (GATHER_MONITOR_STATISTICS) M_INFLATE++;
    }

    /**
     * Construct a monitor using the supplied comparator for maintaining
     * the entry and wait queues.
     *
     */
    protected JavaMonitorImpl(Comparator comp) {
	assert comp != null : "null comparator";
	if (GATHER_MONITOR_STATISTICS) M_INFLATE++;
	this.comp = comp;
	entryQ = new SingleLinkPriorityQueue(comp);
	waitQ = new SingleLinkPriorityQueue(comp);
    }

    /** The factory object */
    public static final Factory factory = new Factory();

    /**
     * The factory class for creating monitors.     
     */
    public static class Factory implements JavaMonitor.Factory {
	public void initialize() {
	    JavaMonitorImpl.initialize();
	}

	public Monitor newInstance() throws PragmaNoPollcheck {
	    return new JavaMonitorImpl();
	}

	public JavaMonitor newJavaMonitorInstance() throws PragmaNoPollcheck {
	    return new JavaMonitorImpl();
	}

	public int monitorSize() throws PragmaNoPollcheck {
	    return JavaMonitorImpl.sizeOf();
	}
    }

    /**
     *  Returns the actual size of an instance of this class, including the
     *  space needed for the object header and all fields, plus the space
     *  needed for creating referenced objects (and transitively the space
     *  they need to create referenced objects) during construction.
     */
    static int sizeOf() {
	return S3Domain.sizeOfInstance("s3/services/java/ulv1/JavaMonitorImpl") + constructionSizeOf();
    }

    /**
     * Returns the maximum space allocated during the execution of the
     * constructor of an instance of this class, and transitively the space
     * needed by any object allocation performed in this constructor.
     * Note this doesn't include "temporary" allocations like debug strings
     * etc, but it does include super constructors. Hence for any class the
     * total space needed to do "new" is the base size plus the construction
     * size.
     */
    protected static int constructionSizeOf() {
	// the only objects constructed are two SingleLinkPriorityQueues.
	// That class performs no allocation during construction so we just
	// need its base size.
	return 2 * S3Domain.sizeOfInstance("s3/util/queues/SingleLinkPriorityQueue");
    }

    /** Flag for controlling debug output. If enabled you can make this
     a per-instance flag. When disabled it has to be a static constant to
     ensure the code is stripped at compile time
     */
    public static final boolean DEBUG = false;

    /* customization hooks */

    /**
     * Executed when a thread trying to acquire the monitor finds that the
     * monitor is already owned. This method is called before the monitor
     * state or the thread state are altered. If the method returns 
     * <tt>true</tt> then we continue with the blocking, otherwise we
     * abort the monitor acquisition.
     * <p><b>FIXME</b>: the pieces aren't quite in place to do tryLock yet.
     *
     * @param current the thread that is trying to acquire the monitor
     * @return <tt>true if the monitor acquisition should continue to block
     * the current thread, and <tt>false</tt> if the monitor acquisition should
     * be aborted.
     */
    protected boolean onBlocking(OVMThread current) throws PragmaNoPollcheck {
	if (GATHER_MONITOR_STATISTICS) M_BLOCK++;
	if (DEBUG) BasicIO.out.println("Block " + current);
return true;
    }

    /**
     * Executed when a thread that needs to reacquire the monitor lock after
     * a wait completes (either normally or otherwise), finds that the monitor
     * is already locked. When this method is called, the thread is not
     * yet in the entry queue for the monitor.
     * @param t the thread that needs to reacquire the monitor
     */
    protected void onBlockingAfterWait(OVMThread t) throws PragmaNoPollcheck {}

    /**
     * Executed when the monitor has been handed off to a waiting thread, but
     * before the new owner is made ready
     */
    protected void onAcquire() throws PragmaNoPollcheck {}

    /**
     * Executed when the monitor is about to be released and there are waiting
     * threads. This method is called
     * before the monitor, or any waiting thread's state, is modified.
     * The current thread is the monitor's owner.
     *
     */
    protected void onExit() throws PragmaNoPollcheck {}

    // warning: we can't use string concat or primitive-to-string conversions
    //          in asserts due to synchronization in the allocation routine
    //          (actually copyNonOverlapping used by System.arraycopy).
    //          If you enable debugging you'll have to disable the sync in
    //          copyNonOverlapping
    // DH 12 Nov 2003

    /* Implementation of JavaMonitor methods */

    public void enter() throws PragmaNoPollcheck, PragmaNoBarriers {
	if (GATHER_MONITOR_STATISTICS) M_ENTER++;
	if (DEBUG) BasicIO.out.println("JavaMonitorImpl entry");
	JavaOVMThreadImpl current = (JavaOVMThreadImpl) jtm.getCurrentThread();
	if (DEBUG) BasicIO.out.println("Monitor entry by thread " + current.getName());

	if (enterQuick(current)) return;

	if (owner == current) {
	    // recursive entry doesn't require atomicity
	    if (DEBUG) BasicIO.out.println("\t>recursive entry: count=" + entryCount);
	    enterRecursive();
	    if (DEBUG) BasicIO.out.println("\t<recursive entry: count=" + entryCount);
	} else { // need atomicity
	    if (GATHER_MONITOR_STATISTICS) M_ACQUIRE++;
	    boolean enabled = jtm.setReschedulingEnabled(false);
	    try {
		if (DEBUG) BasicIO.out.println("\tdoing initial entry");
		doEnter(current);
		if (DEBUG) BasicIO.out.println("Monitor acquired by thread " + current.getName());
	    } finally {
		jtm.setReschedulingEnabled(enabled);
	    }
	}
    }

    public void enter(OVMThread thr) throws PragmaNoBarriers, PragmaNoPollcheck {
	if (owner == null) {
	    owner = (JavaOVMThread) thr;
	    entryCount = 1;
	} else {
	    assert(owner == thr);
	    entryCount++;
	}
    }

    /**
     * Atomically check-for and acquire an unowned monitor. This method uses
     * the fastest atomicity mechanism available - which currently means no
     * poll checks.
     */
    private final boolean enterQuick(JavaOVMThreadImpl current) throws PragmaNoPollcheck, PragmaInline,
	    PragmaNoBarriers {
	if (owner == null) {
	    if (GATHER_MONITOR_STATISTICS) M_ENTER_QUICK++;// FIXME -- CRAP!! This is not what you want...
	    owner = current;
	    entryCount = 1;
	    if (DEBUG) BasicIO.out.println("\tacquiring unowned monitor-quick");
	    return true;
	}
	return false;
    }

    /**
     * Provides the common mechanics used for monitor entry. This should only
     * be called from within an atomic region.
     */
    protected void doEnter(JavaOVMThreadImpl current) throws PragmaNoBarriers, PragmaNoPollcheck {
	if (owner == null) {
	    if (DEBUG) BasicIO.out.println("\tacquiring unowned monitor");
	    owner = current;
	    entryCount = 1;
	} else {
	    if (DEBUG) BasicIO.out.println("\tmonitor owned - calling on-blocking");
	    if (!onBlocking(current)) {
		if (DEBUG) BasicIO.out.println("\ton-blocking => false - returning");
		return;
	    }
	    if (DEBUG) BasicIO.out.println("\ton-blocking => true - continuing");

	    jtm.removeReady(current); // must remove first
	    entryQ.add(current);
	    entrySize++;
	    current.setWaitingMonitor(this);
	    current.setState(JavaOVMThread.BLOCKED_MONITOR);
	    if (DEBUG) BasicIO.out.println("\tthread queued: size = " + entrySize);
	    jtm.runNextThread(); // switch to runnable thread
	    // when we return we are the owner
	    
            if (false) {
              if (owner != current) {
                Native.print_string("ERROR: not monitor owner when woken up in doEnter, current thread is ");
                MemoryManager.the().printAddress(VM_Address.fromObjectNB(current));
                Native.print_string(", monitor owner thread is ");
                MemoryManager.the().printAddress(VM_Address.fromObjectNB(owner));
                Native.print_string(", and the monitor is ");
                MemoryManager.the().printAddress(VM_Address.fromObjectNB(this));
                Native.print_string("\n");
                throw Executive.panic("fix...");
              }
            
              if (entryCount != 1) {
                Native.print_string("ERROR: entry count is not 1 after woken up in doEnter, it is instead ");
                Native.print_int(entryCount);
                Native.print_string("\n");
                throw Executive.panic("fix...");
              }
              
              if (current.getState() != JavaOVMThread.READY) {
                Native.print_string("ERROR: current thread is not ready after woken up in doCenter, current thread is ");
                MemoryManager.the().printAddress(VM_Address.fromObjectNB(current));
                Native.print_string(", and the monitor is ");
                MemoryManager.the().printAddress(VM_Address.fromObjectNB(this));
                Native.print_string("\n");
                throw Executive.panic("fix...");
              }
            } else {
	      assert owner == current && entryCount == 1:
		"wrong monitor state on return";
              assert current.getState() == JavaOVMThread.READY:
		"wrong thread state on return";
           }
	}
    }

    public void enterRecursive() throws PragmaNoPollcheck, PragmaInline {
	if (GATHER_MONITOR_STATISTICS) M_RECURSIVE++;
	// assert: owner = currentThread 
	entryCount++;
    }

    public void exit() {
	JavaOVMThread current = (JavaOVMThread) jtm.getCurrentThread();
	if (DEBUG) BasicIO.out.println("Monitor exit by thread " + current.getName());
	checkOwner(current);
	if (DEBUG) BasicIO.out.println("\tcurrent thread is owner");
	if (entryCount == 1) {
	    if (exitQuick()) return;
	    // need atomicity
	    boolean enabled = jtm.setReschedulingEnabled(false);
	    try {
		if (DEBUG) BasicIO.out.println("\tdoing final exit");
		doExit();
		if (DEBUG) BasicIO.out.println("Monitor released by thread " + current.getName());
	    } finally {
		jtm.setReschedulingEnabled(enabled);
	    }
	} else {
	    if (DEBUG) BasicIO.out.println("\t> recursive exit: entryCount " + entryCount);
	    exitRecursive();
	    if (DEBUG) BasicIO.out.println("\t< recursive exit: entryCount " + entryCount);
	}
    }

    /**
     * Atomically release an uncontended monitor. This method uses
     * the fastest atomicity mechanism available - which currently means no
     * poll checks.
     */
    private final boolean exitQuick() throws PragmaNoPollcheck, PragmaInline, ovm.core.services.memory.PragmaNoBarriers {
	if (entrySize == 0) {
	    owner = null;
	    entryCount = 0;
	    if (DEBUG) BasicIO.out.println("\tNo waiters - releasing monitor quick");
	    return true;
	}
	return false;
    }

    public void exitRecursive() throws PragmaNoPollcheck, PragmaInline {
	// assert: entryCount > 1
	entryCount--;
    }

    /* this doesn't need sync as owner cannot change to be the current
     thread, nor can it change from being the current thread
     */
    protected void checkOwner(JavaOVMThread current) throws IllegalMonitorStateException {
	if (owner != current) { throw new IllegalMonitorStateException(); }
    }

    /** 
     * Perform the common mechanics of releasing the monitor. This should
     * only be called from an atomic region by the owning thread.
     *
     */
    protected void doExit() throws ovm.core.services.memory.PragmaNoBarriers, PragmaNoPollcheck {
	assert entryCount == 1 : "releasing too early";
	if (entrySize > 0) {
	    if (DEBUG) BasicIO.out.println("\tcalling onExit as entrySize>0");
	    onExit();
	    if (DEBUG) {
		BasicIO.out.println("\tonExit completed");
		BasicIO.out.println("\twaking up queued thread: entrySize = " + entrySize);
	    }
	    owner = (JavaOVMThread) entryQ.take();
	    if (DEBUG) BasicIO.out.println("\tnew owner is " + owner.getName());
	    onAcquire();
	    entrySize--;
	    owner.setState(JavaOVMThread.READY);
	    owner.setWaitingMonitor(null);
	    jtm.makeReady(owner);
	} else {
	    if (DEBUG) BasicIO.out.println("\tNo waiters - releasing monitor");
	    owner = null;
	    entryCount = 0;
	}
    }

    public void signal() {
	JavaOVMThread current = (JavaOVMThread) jtm.getCurrentThread();
	if (DEBUG) BasicIO.out.println("Monitor signal by thread " + current.getName());
	checkOwner(current);
	// need atomicity
	boolean enabled = jtm.setReschedulingEnabled(false);
	try {
	    signalOne();
	    if (DEBUG) BasicIO.out.println("Monitor signal complete");
	} finally {
	    jtm.setReschedulingEnabled(enabled);
	}
    }

    /**
     * The mechanics for waking up one waiting thread. This must
     * be called within an atomic region
     */
    protected void signalOne() throws PragmaNoPollcheck {
	JavaOVMThreadImpl thread = (JavaOVMThreadImpl) waitQ.take();
	if (thread != null) {
	    if (DEBUG) BasicIO.out.println("\tsignalling thread " + thread.getName());
	    waitSize--;
	    // if a timed-wait then remove from timedWait queue
	    int state = thread.getState();
	    if (state == JavaOVMThread.BLOCKED_TIMEDWAIT) {
		if (DEBUG) BasicIO.out.println("\tremoving from timedWaitQ");
		removeFromTimedWaitQueue(thread);
	    } else
		assert state == JavaOVMThread.BLOCKED_WAIT:
		    "waiting thread in wrong state";
	    assert thread.getUnblockState() == JavaOVMThread.UNBLOCKED_NORMAL:
		"wrong unblock state: " + thread.getUnblockState();

	    onBlockingAfterWait(thread);
	    // add to entry queue and adjust state
	    entryQ.add(thread);
	    entrySize++;
	    thread.setState(JavaOVMThread.BLOCKED_MONITOR);
	    thread.setWaitingMonitor(this);
	    thread.setWaitingConditionQueue(null);
	    if (DEBUG) BasicIO.out.println("\tadded to entry queue");
	} else {
	    if (DEBUG) BasicIO.out.println("\tsignalOne called on empty waitQueue!");
	}
    }

    /* It would be nice if doing a signalAll simply merged the waitQ with
     the entry queue. Unfortunately, we need to examine each thread to see
     if it needs to be removed from the timedWaitQueue (and remove it) and
     update each thread's state. Another possible approach would be to have
     separate wait and timed-wait priority queues and then we would only
     need to do thread-by-thread for the timed-wait case, but we'd also have
     to check both queues when doing a signal to see which has the highest
     priority waiter. This could introduce non-determinancies when both
     waiters have the same priority.
     */
    public void signalAll() {
	JavaOVMThread current = (JavaOVMThread) jtm.getCurrentThread();
	if (DEBUG) BasicIO.out.println("Monitor signalAll by thread " + current.getName());
	checkOwner(current);
	// need atomicity
	boolean enabled = jtm.setReschedulingEnabled(false);
	try {
	    while (waitSize > 0) {
		signalOne();
	    }
	    if (DEBUG) BasicIO.out.println("Monitor signalAll complete");
	} finally {
	    jtm.setReschedulingEnabled(enabled);
	}
    }

    public boolean waitAbortable(Monitor ignored) {
	JavaOVMThreadImpl current = (JavaOVMThreadImpl) jtm.getCurrentThread();
	if (DEBUG) BasicIO.out.println("Monitor waitAbortable by thread " + current.getName());
	checkOwner(current);
	// need atomicity
	boolean enabled = jtm.setReschedulingEnabled(false);
	try {
	    // watch for an already interrupted Java thread
	    if (current.isInterrupted()) {
		if (DEBUG) BasicIO.out.println("\tinterrupted on entry: aborting");
		return false;
	    }
	    int count = entryCount;
	    entryCount = 1; // ready for doExit()
	    jtm.removeReady(current);
	    waitQ.add(current);
	    waitSize++;
	    current.setWaitingConditionQueue(this);
	    current.setState(JavaOVMThread.BLOCKED_WAIT);
	    if (DEBUG) BasicIO.out.println("\tadded to wait queue");
	    doExit();
	    jtm.runNextThread(); // switch to runnable thread
	    // when we get here we have the monitor again
	    assert owner == current && entryCount == 1:
		"wrong monitor state on return";
	    assert current.getState() == JavaOVMThread.READY:
		"wrong thread state on return";
	    entryCount = count; // restore count
	    if (DEBUG) BasicIO.out.println("\treacquired monitor");
	    int unblockState = current.getUnblockState();
	    if (unblockState == JavaOVMThread.UNBLOCKED_INTERRUPTED) {
		current.setUnblockState(JavaOVMThread.UNBLOCKED_NORMAL);
		if (DEBUG) BasicIO.out.println("Wait completed due to interrupt");
		return false;
	    } else {
		assert unblockState == JavaOVMThread.UNBLOCKED_NORMAL:
		    "wrong unblockState";
		if (DEBUG) BasicIO.out.println("Wait completed normally");
		return true;
	    }
	} finally {
	    jtm.setReschedulingEnabled(enabled);
	}
    }

    public int waitTimedAbortable(Monitor ignored, long timeout) {
	JavaOVMThreadImpl current = (JavaOVMThreadImpl) jtm.getCurrentThread();
	if (DEBUG) BasicIO.out.println("Monitor waitTimedAbortable by thread " + current.getName());
	checkOwner(current);

	// need to adjust timeout to ensure we sleep >= timeout regardless of
	// the actual clock update granularity. This means we must round up
	// to an even multiple of sleepMin and add sleepMin again to ensure
	// we wait at least two clock ticks (the first of which could happen
	// just after we go in the queue, thus reducing timeout by sleepMin
	// straight away)
	// NOTE: if events have been disabled for a while and we haven't hit
	// a pollcheck since they were enabled, then there could be multiple
	// outstanding ticks which still cause this sleep to return 
	// prematurely. DH 25/8/2004

	long rem = timeout % sleepMin;
	if (rem > 0) timeout = timeout - rem + 2 * sleepMin;
	else timeout = timeout + sleepMin;

	// need atomicity
	boolean enabled = jtm.setReschedulingEnabled(false);
	try {
	    // watch for an already interrupted Java thread
	    if (current.isInterrupted()) {
		if (DEBUG) BasicIO.out.println("\tinterrupted on entry: aborting");
		return ABORTED;
	    }
	    if (timeout == 0) { // balk - but this should never happen
		return TIMED_OUT;
	    }
	    int count = entryCount;
	    entryCount = 1; // ready for doExit()
	    jtm.removeReady(current);
	    waitQ.add(current);
	    waitSize++;
	    if (DEBUG) BasicIO.out.println("\tadded to wait queue");
	    current.setVisitor(timeoutAction);
	    timer.delayRelative(current, timeout);
	    if (DEBUG) BasicIO.out.println("\tadded to relative delay queue");
	    current.setWaitingConditionQueue(this);
	    current.setState(JavaOVMThread.BLOCKED_TIMEDWAIT);
	    doExit();
	    jtm.runNextThread(); // switch to runnable thread
	    // when we get here we have the monitor again
	    assert owner == current && entryCount == 1:
		"wrong monitor state on return";
	    assert current.getState() == JavaOVMThread.READY:
		"wrong thread state on return";
	    entryCount = count; // restore count
	    if (DEBUG) BasicIO.out.println("\treacquired monitor");
	    int unblockState = current.getUnblockState();
	    if (unblockState == JavaOVMThread.UNBLOCKED_INTERRUPTED) {
		current.setUnblockState(JavaOVMThread.UNBLOCKED_NORMAL);
		if (DEBUG) BasicIO.out.println("Wait completed due to interrupt");
		return ABORTED;
	    } else if (unblockState == JavaOVMThread.UNBLOCKED_TIMED_OUT) {
		current.setUnblockState(JavaOVMThread.UNBLOCKED_NORMAL);
		if (DEBUG) BasicIO.out.println("Wait completed by timeout");
		return TIMED_OUT;
	    } else {
		assert unblockState == JavaOVMThread.UNBLOCKED_NORMAL:
		    "wrong unblock state";
		if (DEBUG) BasicIO.out.println("Wait completed normally");

		return SIGNALLED;
	    }
	} finally {
	    jtm.setReschedulingEnabled(enabled);
	}
    }

    /**
     * Aborts the wait of the specified thread on this condition.
     * @return <code>true</code> as this implementation only has
     * abortable wait points.
     */
    public boolean abortWait(OVMThread t) throws PragmaNoBarriers {

	// If the timer code calls this then the target thread has already
	// been removed from the timed wait queue. To avoid looking for it, the
	// timer code changes it's state to be just BLOCKED_WAIT before calling
	// this method.

	JavaOVMThreadImpl thread = (JavaOVMThreadImpl) t;
	if (DEBUG) BasicIO.out.println("Abort wait called for thread: " + thread.getName());
	boolean enabled = jtm.setReschedulingEnabled(false);
	try {
	    int state = thread.getState();
	    assert (state == JavaOVMThread.BLOCKED_WAIT
		    || state == JavaOVMThread.BLOCKED_TIMEDWAIT):
		"incorrect thread state";

	    if (waitQ.remove(thread)) {
		if (DEBUG) BasicIO.out.println("\tremoved from waitQ");
		waitSize--;
		thread.setWaitingConditionQueue(null);
	    } else {
		throw new OVMError.Internal("aborted thread not in waitQueue");
	    }

	    if (state == JavaOVMThread.BLOCKED_TIMEDWAIT) {
		removeFromTimedWaitQueue(thread);
	    }
	    // now try to hand the monitor to the aborted thread
	    // note we can't use doEnter as it's not the current thread and so
	    // the logic is inverted
	    if (owner == null) {
		if (DEBUG) BasicIO.out.println("\thanding over unowned monitor");
		assert entryCount == 0 : "wrong entry count for unowned monitor";
		owner = thread;
		entryCount = 1;
		owner.setState(JavaOVMThread.READY);
		jtm.makeReady(owner);
	    } else {
		if (DEBUG) BasicIO.out.println("\tblocking on monitor reacquire");
		onBlockingAfterWait(thread);
		entryQ.add(thread);
		entrySize++;
		thread.setState(JavaOVMThread.BLOCKED_MONITOR);
		thread.setWaitingMonitor(this);
	    }
	    if (DEBUG) BasicIO.out.println("Abort wait complete");
	} finally {
	    jtm.setReschedulingEnabled(enabled);
	}
	return true;
    }

    /**
     * Mechanics for removing an aborted timed-wait from the relevant queues.
     */
    protected void removeFromTimedWaitQueue(JavaOVMThreadImpl thread) {
	if (DEBUG) BasicIO.out.println("\tremoving from timedWaitQ");
	if (!timer.wakeUpRelative(thread)) { throw new OVMError.Internal("thread not in relative delay queue"); }
	if (DEBUG) BasicIO.out.println("\tremoved from relative delay queue");
    }

    public int getEntryQueueSize() {
	return entrySize;
    }

    public OVMThread getOwner() {
	return owner;
    }

    public boolean isEntering(OVMThread t) {
	boolean enabled = jtm.setReschedulingEnabled(false);
	try {
	    return entryQ.contains((JavaOVMThreadImpl) t);
	} finally {
	    jtm.setReschedulingEnabled(enabled);
	}
    }

    public int entryCount() {
	return entryCount;
    }

    public int getWaitQueueSize() {
	return waitSize;
    }

    public boolean isWaiting(OVMThread t) {
	boolean enabled = jtm.setReschedulingEnabled(false);
	try {
	    return waitQ.contains((JavaOVMThreadImpl) t);
	} finally {
	    jtm.setReschedulingEnabled(enabled);
	}
    }

    /* Implementation of Ordered methods */

    /**
     * The comparator must be set via a constructor argument or else the
     * default thread manager comparator is used.
     * @throws IllegalStateException always
     *
     */
    public void setComparator(Comparator comp) {
	throw new OVMError.IllegalState("comparator can not be set");
    }

    public Comparator getComparator() {
	return comp;
    }

    // This must only be called from within an atomic region
    public void changeNotification(Object o) throws PragmaNoPollcheck {
	JavaOVMThread thread = (JavaOVMThread) o;
	int state = thread.getState();
	switch (state) {
	case JavaOVMThread.BLOCKED_MONITOR:
	    entryQ.changeNotification(thread);
	    break;
	case JavaOVMThread.BLOCKED_WAIT:
	case JavaOVMThread.BLOCKED_TIMEDWAIT:
	    waitQ.changeNotification(thread);
	    break;
	default:
	    throw new OVMError.Internal("wrong state for monitor change notification: " + state);
	}
    }

    // additional testing/debugging hook
    public SingleLinkPriorityQueue getEntryQueue() {
	return entryQ;
    }

    public static void dumpMonitorStatistics() {
	if (!GATHER_MONITOR_STATISTICS) return;
	Native.print_string("\nSwitch " + M_SWITCH);
	Native.print_string("\nEnter " + M_ENTER);
	Native.print_string("\nEnter Quick " + M_ENTER_QUICK);
	Native.print_string("\nInflate " + M_INFLATE);
	Native.print_string("\nEnter Recursive " + M_RECURSIVE);
	Native.print_string("\nBlock " + M_BLOCK);
    }

}
