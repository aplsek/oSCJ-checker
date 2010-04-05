package s3.services.java.realtime;

import ovm.core.domain.Oop;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.timer.TimeConversion;
import ovm.services.java.JavaMonitor;
import ovm.services.java.JavaOVMThread;
import ovm.services.java.realtime.RealtimeJavaDispatcher;
import ovm.services.realtime.RealtimeOVMThread;
import ovm.services.threads.PriorityOVMThread;
import ovm.util.OVMError;
import s3.core.domain.S3RealtimeJavaUserDomain;
import s3.services.java.ulv1.JavaDispatcherImpl;
import s3.util.PragmaInline;
import s3.util.PragmaNoPollcheck;
import ovm.core.services.memory.ScopedMemoryPolicy;
import ovm.core.services.memory.MemoryPolicy;

/**
 * An extension of the {@link JavaDispatcherImpl} that includes the
 * functionality of the 
 * {@link s3.services.realtime.RealtimeDispatcherImpl} and the
 * {@link s3.services.realtime.PriorityInheritanceDispatcherImpl}. As we
 * don't have multiple inheritance we have to duplicate the code directly.
 *
 * <p>This implementation is tightly coupled with 
 * {@link RealtimeJavaThreadImpl} to perform thread finalization.
 * See {@link #onTermination} for details.
 *
 * <h3>Notes</h3>
 * <p>A real-time dispatcher defines separate priority ranges for
 * real-time and non-real-time OVM threads. In a real-time Java configuration
 * <b>all</b> Java threads are bound to real-time OVM threads (as all threads
 * have to support priority inheritance and so could take on any priority).
 * The Java level real-time dispatcher will establish the Java-level priority
 * ranges by querying what is available from this kernel-level dispatcher. It
 * will require that we support at least 29 priority levels: 28 for the RTSJ
 * plus one for normal Java threads. In practice we support 40 priority levels
 * as this allows for all normal and RT Java priorities plus two 'system' 
 * priorities than could be used by GC or low-level event handling threads.
 *
 * <p>A thread with a delayed start time is still considered to
 * be alive while waiting for that start time. This fits with the definition
 * of <tt>java.lang.Thread.isAlive()</tt> which states that a thread is alive
 * if start has been invoked on it (and it hasn't terminated). This treats a
 * delayed start as if the thread had a sleep() call inserted as its first
 * instruction - except we put it on the sleep queue directly.
 *
 * <p>All methods that disable rescheduling are declared PragmaNoPollcheck to
 * avoid redundant poll checks during those methods. We don't use PragmaAtomic
 * because we know how to work with the thread manager, and PragmaAtomic might
 * (in theory) do more or less than what we need).
 *
 * <h3>To Do</h3>
 * <p>Deal with blocking I/O.
 *
 * @author David Holmes
 *
 */
public class RealtimeJavaDispatcherImpl extends JavaDispatcherImpl
    implements RealtimeJavaDispatcher {


    /** The singleton instance of this class */
    final static RealtimeJavaDispatcher instance = new RealtimeJavaDispatcherImpl();

//      static {
//  	new Error("RT code dragged in").printStackTrace(System.err);
//      }

    /**
     * Return the singleton instance of this class 
     * @return the singleton instance of this class 
     */
    public static OVMDispatcher getInstance() {
        return instance;
    }


    /** no construction allowed */
    protected RealtimeJavaDispatcherImpl() {}


    /**
     * Override to set the comparator for the real-time Java thread.
     */
    public void init() {
        super.init();
        RealtimeJavaThreadImpl.realComp = comp;
    }


    // NOTE: when running a RT-JVM *all* java threads are RT threads in
    // the kernel. But we also allow for non-RT kernel threads even if not
    // presently used.

    // allow a reasonable non-RT range
    protected static final int MIN_NON_RT_PRIORITY = -100;
    protected static final int MAX_NON_RT_PRIORITY = 0;

    // RT range is typically:
    //   1-10  normal Java
    //   11    non-RT System priority (eg. GC)
    //   12-39 RTSJ priorities
    //   40-42 RT system priorities (eg. low-level event handler, RTGC)
    protected static final int MIN_RT_PRIORITY = 1;
    protected static final int MAX_RT_PRIORITY = 42;

    public int getMinPriority() {
        return MIN_NON_RT_PRIORITY;
    }

    public int getMaxPriority() {
        return MAX_NON_RT_PRIORITY;
    }

    public int getMinNonRTPriority() {
        return MIN_NON_RT_PRIORITY;
    }

    public int getMaxNonRTPriority() {
        return MAX_NON_RT_PRIORITY;
    }

    public int getMinRTPriority() {
        return MIN_RT_PRIORITY;
    }

    public int getMaxRTPriority() {
        return MAX_RT_PRIORITY;
    }


    public boolean isValidNonRTPriority(int priority) {
        return priority >= MIN_NON_RT_PRIORITY && 
               priority <= MAX_NON_RT_PRIORITY;
    }

    public boolean isValidRTPriority(int priority) {
        return priority >= MIN_RT_PRIORITY && 
               priority <= MAX_RT_PRIORITY;
    }


    // override to distinguish betwen RT and non-RT threads
    public void setPriority(PriorityOVMThread thread, int prio) 
        throws PragmaNoPollcheck {
        if (thread instanceof RealtimeOVMThread) {
            setPriority((RealtimeOVMThread)thread, prio);
        }
        else {
            if (isValidNonRTPriority(prio)) {
                super.setPriority(thread, prio);
            }
            else {
            throw new OVMError.IllegalArgument(
                "nonRT priority out of range: min("
                + MIN_NON_RT_PRIORITY + ") -> " 
                + prio + " -> max(" 
                + MAX_NON_RT_PRIORITY + ")" );
            }
        }
    }


    /**
     * Sets the base priority of the given thread to the given priority.
     * <p>This overrides our inherited <tt>setPriority</tt> method to
     * ensure we only set the base priority directly. The active priority
     * is set indirectly.
     */
    public void setPriority(RealtimeOVMThread thread, int prio) 
        throws PragmaNoPollcheck {
        if (isValidRTPriority(prio)) {
            setBasePriority((JavaOVMThread)thread, prio);
        }
        else {
            throw new OVMError.IllegalArgument(
                "RT priority out of range: min("
                + MIN_RT_PRIORITY + ") -> " 
                + prio + " -> max(" 
                + MAX_RT_PRIORITY + ")" );
        }
    }

    public boolean startThreadDelayed(OVMThread thread, long releaseTime) 
        throws PragmaNoPollcheck {
        JavaOVMThread jthread = (JavaOVMThread) thread;
        boolean enabled = tm.setReschedulingEnabled(false);
        try {
            // BasicIO.out.println("startThreadDelayed: " + jthread.getName() +
            //                     " at " + releaseTime);
            nThreads++;
            // prepare the thread for starting regardless
            jthread.prepareForStart(this);
            // mark the thread as sleeping but uninterruptible.
            // when it wakes it will be made ready.
            jthread.setState(JavaOVMThread.BLOCKED_SLEEP_NOINTERRUPT);
            if (!jtm.sleepAbsolute(thread, releaseTime) ){
                // start time has passed so start immediately
                jthread.setState(JavaOVMThread.READY);
                jtm.makeReady(jthread);
                return false;
            }
            else {
                return true;
            }
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }
    }


    public void delayCurrentThreadUninterruptible(long millis, int nanos) 
     throws PragmaNoPollcheck {
        JavaOVMThread current = (JavaOVMThread) jtm.getCurrentThread();
        boolean enabled = jtm.setReschedulingEnabled(false);
        try {
            current.setState(JavaOVMThread.BLOCKED_SLEEP_NOINTERRUPT);
            jtm.sleep(current, nanos + millis*TimeConversion.NANOS_PER_MILLI);
            current.setState(JavaOVMThread.READY);
	    assert current.getUnblockState() ==  JavaOVMThread.UNBLOCKED_NORMAL:
		"wrong unblock state";
        }
        finally {
            jtm.setReschedulingEnabled(enabled);
        }
    }

    public int delayCurrentThreadAbsolute(long wakeupTime)  
        throws PragmaNoPollcheck {
        return delayAbsolute(wakeupTime, true);
    }

    public int delayCurrentThreadAbsoluteUninterruptible(long wakeupTime) 
        throws PragmaNoPollcheck {
        return delayAbsolute(wakeupTime, false);
    }

    // internal mechanics for absolute delays
    protected int delayAbsolute(long nanos, boolean canInterrupt) 
        throws PragmaNoPollcheck {
        JavaOVMThread current = (JavaOVMThread) jtm.getCurrentThread();
        boolean enabled = tm.setReschedulingEnabled(false);
        try {
            if (canInterrupt) {
                if (current.isInterrupted()) {
                    return ABSOLUTE_INTERRUPTED;
                }
                current.setState(JavaOVMThread.BLOCKED_SLEEP);
            }
            else {
                current.setState(JavaOVMThread.BLOCKED_SLEEP_NOINTERRUPT);
            }
            if (!jtm.sleepAbsolute(current, nanos)) {
                // didn't get put on sleep queue so we're ready to execute
                current.setState(JavaOVMThread.READY);
                return ABSOLUTE_PAST;
            }
            else {
                assert current.getState() == JavaOVMThread.READY:
		    "thread not ready after absolute sleep";
                // check why we wokeup
                if (canInterrupt && 
                    current.getUnblockState() == 
                        JavaOVMThread.UNBLOCKED_INTERRUPTED) {
                    current.setUnblockState(JavaOVMThread.UNBLOCKED_NORMAL);
                    return ABSOLUTE_INTERRUPTED;
                }
                else {
                    assert (current.getUnblockState() ==
			    JavaOVMThread.UNBLOCKED_NORMAL):
			((canInterrupt ? "" : "non-") +
			 "interruptible sleep had unblock state: " +
			 current.getUnblockState());
                    return ABSOLUTE_NORMAL;
                }
            }
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }
    }


    /**
     * Atomically queries the state of the thread and sets a new priority
     * value if the thread is in the appropriate state.
     * <p>The first version of the RTSJ prevents changing priority on a thread
     * that isn't doing a wait() or sleep(). This causes us problems when
     * trying to keep the params for a bound async event handler and its 
     * thread 'synchronized'. In the 1.0.1 release this restriction has been
     * removed so we remove it now. - DH 20 Nov. 2003
     *
     * @param thread the thread for which the base priority is to be 
     * changed, if allowed.
     * @param prio the new base (and possibly active) priority for the thread
     *
     * @return <tt>true</tt> if the thread is in the right state to set the
     * priority, and <tt>false</tt> otherwise.
     */
    public boolean setPriorityIfAllowed(JavaOVMThread thread, int prio) 
        throws PragmaNoPollcheck {
        boolean enabled = jtm.setReschedulingEnabled(false);
        try {
            int state = thread.getState();
            assert (state != JavaOVMThread.NOT_STARTED &&
		    state != JavaOVMThread.TERMINATED): "thread not alive";

            // any state ok
            setBasePriority(thread, prio);
            return true;
        }
        finally {
            jtm.setReschedulingEnabled(enabled);
        }
    }

    /**
     * Sets the base priority of the given thread to the given value.
     * If required, the active priority of the thread is updated and so
     * a reschedule may occur.
     * <p>This should only be called for threads that are alive.
     * <p>The new priority value is assumed to be in range due to higher-level
     * validity checks.
     *
     * @param thread the thread for which the base priority is to be 
     * changed.
     * Note that although this must be an instance of
     * {@link RealtimeJavaThreadImpl} we leave the type as the
     * more generic {@link JavaOVMThread} to avoid polluting the 
     * higher-level clients with implementation details.
     * @param prio the new base (and possibly active) priority for the thread
     */
    public void setBasePriority(JavaOVMThread thread, int prio) 
        throws PragmaNoPollcheck {
        RealtimeJavaThreadImpl piThread = 
            (RealtimeJavaThreadImpl) thread;

        boolean enabled = jtm.setReschedulingEnabled(false);
        try {
            int oldBase = piThread.getBasePriority();
            piThread.setBasePriority(prio);
            if (oldBase != prio) {  // actual priority change?
                maintainPriorityRelations(piThread);
            }
        }
        finally {
            jtm.setReschedulingEnabled(enabled);
        }
    }

    // warning: this produces huge amounts of data
    // warning: turning this on may need synchronization hacks to avoid recursion
    static final boolean DEBUG_SYNC = false;

    /**
     * Updates the active priority of the given thread and ensures that
     * all priority inheritance relationships are updated to reflect that
     * priority change.
     * <p>This method should only be called within an atomic region.
     *
     * @param thread the thread that needs updating
     *
     */
    final void maintainPriorityRelations(RealtimeJavaThreadImpl thread) 
        throws PragmaNoPollcheck {

        // ##### NOTE ####
        // This method is part of the synchronization implementation.
        // It must not invoke directly or indirectly any synchronized code
        // This means that we must avoid any toString() methods that would
        // use getClass().getName() as the repository methods use sync.
        // We must also avoid string concat as arraycopy also now uses sync
        // - DH 19 Nov. 2003

        if (DEBUG_SYNC) 
            BasicIO.out.println("maintainingPriorityRelations for " + thread.getName());

        int activePriority = thread.getCurrentActivePriority();
        if (activePriority  == thread.getPriority()) {
            // no actual change, so do nothing
            if (DEBUG_SYNC) 
                BasicIO.out.println("\tno priority change so nothing to do");
            return;
        }

        if (DEBUG_SYNC) {
            BasicIO.out.println("\tsetting priority: current = " + 
                                thread.getPriority() + ", active = " + 
                                activePriority);
        }
        thread.setPriority(activePriority);

        // what we do now depends on the state of the thread. If we're in
        // an ordered queue then we must get re-ordered. If we're blocked
        // entering a monitor then we have to see if our priority change
        // should be inherited by the monitor owner.

        int state = thread.getState();

        switch(state) {
            case JavaOVMThread.BLOCKED_SLEEP:
            case JavaOVMThread.BLOCKED_SLEEP_NOINTERRUPT:
            {
                assert jtm.isSleeping(thread):  "SLEEP thread not sleeping!";
                // nothing else to do for a sleep
                if (DEBUG_SYNC) 
                    BasicIO.out.println("\t-sleeping so nothing to do");
                return;
            }
            case JavaOVMThread.BLOCKED_WAIT:
            case JavaOVMThread.BLOCKED_TIMEDWAIT:
            {
                JavaMonitor mon = 
                    (JavaMonitor)thread.getWaitingConditionQueue();
                assert mon != null : "null monitor when waiting";
                if (DEBUG_SYNC) 
                    BasicIO.out.println("\t-waiting so reordering queue");
                mon.changeNotification(thread);
                return;
            }
            case JavaOVMThread.BLOCKED_MONITOR:
            {
                // FIXME when we support different monitor control policies, 
                // this will have to test for a PrioInherit monitor.
                PriorityInheritanceJavaMonitorImpl mon = 
                    (PriorityInheritanceJavaMonitorImpl) 
                    thread.getWaitingMonitor();
                assert mon != null : "null monitor when blocked";
                if (DEBUG_SYNC) 
                    BasicIO.out.println("\t-waiting on a monitor");
                // we're waiting on a monitor so reorder the monitor queue
                // and if the head waiter has changed then maintain the
                // priority relations of owner. If the head waiter hasn't 
                // changed and its this thread then we need to reorder the
                // owners priority inheritance queue then maintain the 
                // priority relations of the owner
                RealtimeJavaThreadImpl oldHead = 
                    (RealtimeJavaThreadImpl) mon.getEntryQueue().head();
                if (DEBUG_SYNC) 
                    BasicIO.out.println("\t\t old head =" + oldHead.getName());
                mon.changeNotification(thread);
                RealtimeJavaThreadImpl newHead = 
                    (RealtimeJavaThreadImpl) mon.getEntryQueue().head();
                if (DEBUG_SYNC) 
                    BasicIO.out.println("\t\t new head =" + newHead.getName());
                RealtimeJavaThreadImpl owner = 
                    (RealtimeJavaThreadImpl) mon.getOwner();
                if (DEBUG_SYNC) 
                    BasicIO.out.println("\t\t owner =" + owner.getName());
                if (oldHead != newHead) {
                    if (DEBUG_SYNC) 
                        BasicIO.out.println("\t\t old head != new head");
		    assert owner.inheritanceQueue.remove(oldHead.node):
			"old head not in owner's inheritance queue";
                    if (DEBUG_SYNC) 
                        BasicIO.out.println("\t\t\t-removed old head from owners inheritance queue ");
                    assert !owner.inheritanceQueue.contains(newHead.node):
			"new head already in owner's inheritance queue";
                    owner.inheritanceQueue.add(newHead.node);
                    if (DEBUG_SYNC) 
                        BasicIO.out.println("\t\t\t-added new head to owners inheritance queue");
                    maintainPriorityRelations(owner);
                }
                else if (oldHead == thread) {
                    if (DEBUG_SYNC) 
                        BasicIO.out.println("\t\t old head is thread - reordering owners inheritance queue");
                    assert owner.inheritanceQueue.contains(thread.node):
			"thread should be in owner's inheritance queue";
                    owner.inheritanceQueue.changeNotification(thread.node);
                    maintainPriorityRelations(owner);
                }
                else {
                    // else we haven't changed anything
                    if (DEBUG_SYNC) 
                        BasicIO.out.println("\t\t no changes to monitor queue head or its priority");
                }
                return;
            }
            case JavaOVMThread.TERMINATED:
                // special case: a dying thread is undergoing finalization.
                // the thread has already been marked as terminated but is
                // still in the ready queue
                if (DEBUG_SYNC)
                    BasicIO.out.println("\t-\"terminated\" thread");
                // fall through
            case JavaOVMThread.READY:
            {
                if (DEBUG_SYNC) 
                    BasicIO.out.println("\t-ready thread so reordering run queue");
                assert jtm.isReady(thread):
		    "Thread in READY state missing from ready queue";
                jtm.changeNotification(thread);
                return;
            }
            case JavaOVMThread.NOT_STARTED:
                throw new OVMError.Internal("impossible: thread not started");

            case JavaOVMThread.BLOCKED_IO:
                if (DEBUG_SYNC)
                    BasicIO.out.println("\t-thread blocked on I/O - don't know how to handle that yet");
                return;
            default:
                throw new OVMError.Internal("can't deal with state: " + state);
        }
    }


    /**
     * Returns the base priority of the given thread.
     * This query is atomic with respect to setting the base priority.
     *
     * @param thread the thread to retrieve the base priority of.
     *
     * @return the base priority of <tt>thread</tt>
     */
    public int getBasePriority(JavaOVMThread thread)
        throws PragmaNoPollcheck {
        RealtimeJavaThreadImpl piThread = 
            (RealtimeJavaThreadImpl) thread;
        boolean enabled = jtm.setReschedulingEnabled(false);
        try {
            return piThread.getBasePriority();
        }
        finally {
            jtm.setReschedulingEnabled(enabled);
        }
    }


    // override to return base priority not active
    /**
     * Returns the base priority of the given thread.
     */
    public int getPriority(PriorityOVMThread thread)  throws PragmaNoPollcheck {
        return getBasePriority((JavaOVMThread)thread);
    }

    /**
     * Sets the initial base and active priority of the primordial
     * thread. This will be changed when the primordial Java thread
     * is bound to this VM thread.
     */
    protected void initPrimordialThread(OVMThread primordialThread) {
        RealtimeJavaThreadImpl t =
            (RealtimeJavaThreadImpl) primordialThread;
        int initialPriority = getMinRTPriority();
        t.setPriority(initialPriority);
        t.setBasePriority(initialPriority);
        t.setName("Primordial RT-Java OVM thread");
        d(t.getName() + " base and active priority set to " + initialPriority);
    }


    /**
     * {@inheritDoc}
     * Overrides the inherited version to ensure the VM thread is created in
     * the same allocation context as the Java thread.
     */
    public JavaOVMThread createVMThread(Oop javaThread) throws PragmaInline {
        return createVMThread(javaThread, false);
    }

    /** 
     * Creates the executive domain (VM) thread associated with the given
     * Java thread. The ED thread is created in a private region that
     * the Java thread holds a reference to. When the ED thread (and thus the
     * Java thread) terminates then the region is destroyed, thus destroying
     * the ED thread object too.
     */
    public JavaOVMThread createVMThread(Oop javaThread, boolean noHeap) {
        VM_Area current = MemoryManager.the().getCurrentArea();
	VM_Area target =
		((ScopedMemoryPolicy) MemoryPolicy.the()).makeScratchPadArea();
        MemoryManager.the().setCurrentArea(target);
        try {
            return ((RealtimeJavaThreadImpl.Factory)factory).
                    newInstance(javaThread, noHeap);
        }
        catch(OutOfMemoryError oome) {
            ovm.core.execution.Native.print_string("\nThread area too small!!!\n");
            throw oome;
        }
        finally {
            MemoryManager.the().setCurrentArea(current);
        }
    }


    /** 
     * Termination hook to invoke the current <tt>RealtimeJavaThreadImpl</tt>
     * {@link RealtimeJavaThreadImpl#finalizeThread finalizeThread} method.
     * We also set up for the clean-up of the terminated thread's allocation
     * scope
     */
    protected void onTermination(OVMThread current) throws PragmaNoPollcheck,
                                                           PragmaNoBarriers {
        RealtimeJavaThreadImpl jThread = (RealtimeJavaThreadImpl) current;
        jThread.finalizeThread();
    }
}

