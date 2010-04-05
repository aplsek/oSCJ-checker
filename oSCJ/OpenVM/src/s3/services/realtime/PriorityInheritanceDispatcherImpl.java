/* 
 * $Header: /p/sss/cvs/OpenVM/src/s3/services/realtime/PriorityInheritanceDispatcherImpl.java,v 1.14 2006/04/20 15:48:41 baker29 Exp $
 *
 */
package s3.services.realtime;

import java.util.Comparator;

import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.OVMThread;
import ovm.services.realtime.RealtimePriorityDispatcher;
import ovm.services.threads.PriorityOVMThread;
import ovm.util.Ordered;
/**
 * An extension of the {@link RealtimeDispatcherImpl real-time priority 
 * dispatcher} that supports an
 * implementation of the priority inheritance protocol.
 * The support for the priority inheritance protocol is not
 * general purpose (hence no interface types capture the semantics) but a
 * specific implementation that works in conjunction with the
 * {@link PriorityInheritanceOVMThreadImpl} and the 
 * {@link PriorityInheritanceMonitorImpl} in this package. 
 * This implementation accounts for
 * the arbitrary change of a thread's priority programmatically at runtime.
 *
 * <p>This class can be used in a configuration that does not not require
 * priority inheritance, as it functions as a normal priority dispatcher
 * when accessed through the {@link ovm.services.threads.PriorityOVMDispatcher}
 * interface.
 * When used for priority-inheritance the regular priority dispatcher methods,
 * such as {@link #setPriority} should not be used for threads affected by
 * priority inheritance.
 *
 * @see <a href="doc-files/PriorityInheritance.html">Priority Inheritance Protocol 
 * Implementation</a>
 * @see PriorityInheritanceOVMThreadImpl
 * @see PriorityInheritanceMonitorImpl
 *
 * @author David Holmes
 */
public class PriorityInheritanceDispatcherImpl 
    extends RealtimeDispatcherImpl {

    /** The singleton instance of this class */
    final static RealtimePriorityDispatcher instance = new PriorityInheritanceDispatcherImpl();

    /**
     * Return the singleton instance of this class 
     * @return the singleton instance of this class 
     */
    public static OVMDispatcher getInstance() {
        return instance;
    }

    /**
     * Trivial no-arg constructor
     * @see #init
     */
    protected PriorityInheritanceDispatcherImpl() {}

    /**
     * Override to set the comparator for the PI thread implementation.
     */
    public void init() {
        super.init();
        PriorityInheritanceOVMThreadImpl.realComp = comp;
    }


    /**
     * Sets the base priority of the given thread to the given value.
     * If required, the active priority of the thread is updated and so
     * a reschedule may occur.
     * <p>This should only be called for threads that are alive.
     * <p>The new priority value is assumed to be in range due to higher-level
     * validity checks.
     *
     * @param thread the thread for which the base priority is to be changed.
     * Note that although this must be an instance of
     * {@link PriorityInheritanceOVMThreadImpl} we leave the type as the
     * more generic {@link PriorityOVMThread} to avoid polluting the 
     * higher-level clients with implementation details.
     * @param prio the new base (and possibly active) priority for the thread
     */
    public void setBasePriority(PriorityOVMThread thread, int prio) {
        assert thread instanceof PriorityInheritanceOVMThreadImpl : "need priority inheritance thread type to be using this";
        PriorityInheritanceOVMThreadImpl piThread = 
            (PriorityInheritanceOVMThreadImpl) thread;

        boolean enabled = tm.setReschedulingEnabled(false);
        try {
            int oldBase = piThread.getBasePriority();
            piThread.setBasePriority(prio);
            if (oldBase != prio) {  // actual priority change?
                maintainPriorityRelations(piThread);
            }
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }
    }

    /**
     * Updates the active priority of the given thread and ensures that
     * all priority inheritance relationships are updated to reflect that
     * priority change.
     * <p>Note that this method explicitly does not cause a reschedule to
     * occur. It is up to the caller to force a reschedule after all priority
     * relationships have been re-established.
     * <p>This method should only be called within an atomic region.
     *
     * @param thread the thread that needs updating
     */
    final void maintainPriorityRelations(PriorityInheritanceOVMThreadImpl thread) {
//        BasicIO.out.println("maintainingPriorityRelations for " + thread);

        int activePriority = thread.getCurrentActivePriority();
        if (activePriority  == thread.getPriority()) {
            // no actual change, so do nothing
//            BasicIO.out.println("\tno priority change so nothing to do");
            return;
        }

//        BasicIO.out.println("\tboosting priority");
        thread.setPriority(activePriority);

        // we need to check the state of the thread. There are three
        // general possibilities at present:
        // - sleeping
        // - ready
        // - blocked on a monitor
        //
        // When we have the full Java personality we will track thread
        // state explicitly, but in this incarnation we have to do things
        // a little differently

        if ( sleepMan.isSleeping(thread)) {
            // nothing else to do for a sleep
//            BasicIO.out.println("\t-sleeping so nothing to do");
            return;
        }

        // FIXME when we support different monitor control policies, this will
        // have to test for a PrioInherit monitor.
        PriorityInheritanceMonitorImpl mon = 
            (PriorityInheritanceMonitorImpl) thread.getWaitingMonitor();
        if (mon != null) {
//            BasicIO.out.println("\t-waiting on a monitor");
            // we're waiting on a monitor so reorder the monitor queue
            // and if the head waiter has changed then maintain the
            // priority relations of owner. If the head waiter hasn't changed
            // and its this thread then we need to reorder the
            // owners priority queue then maintain the priority relations
            // of the owner
            PriorityInheritanceOVMThreadImpl oldHead = 
                (PriorityInheritanceOVMThreadImpl) mon.getEntryQueue().head();
//            BasicIO.out.println("\t\t old head =" + oldHead);
            mon.changeNotification(thread);
            PriorityInheritanceOVMThreadImpl newHead = 
                (PriorityInheritanceOVMThreadImpl) mon.getEntryQueue().head();
//            BasicIO.out.println("\t\t new head =" + newHead);
            PriorityInheritanceOVMThreadImpl owner = 
                (PriorityInheritanceOVMThreadImpl) mon.getOwner();
//            BasicIO.out.println("\t\t owner =" + owner);
            if (oldHead != newHead) {
//                BasicIO.out.println("\t\t old head != new head");
                if (!owner.inheritanceQueue.remove(oldHead.node)) {
                    assert false : "old head wasn't in owner's inheritance queue";
                }
//                BasicIO.out.println("\t\t\t-removed old head from owners inheritance queue ");
                assert !owner.inheritanceQueue.contains(newHead.node) : "new head was already in owner's inheritance queue";
                owner.inheritanceQueue.add(newHead.node);
//                BasicIO.out.println("\t\t\t-added new head to owners inheritance queue");
                maintainPriorityRelations(owner);
            }
            else if (oldHead == thread) {
//                BasicIO.out.println("\t\t old head is thread - reordering owners inheritance queue");
                assert owner.inheritanceQueue.contains(thread.node) : "thread should already be in owner's inheritance queue";
                owner.inheritanceQueue.changeNotification(thread.node);
                maintainPriorityRelations(owner);
            }
            else {
                // else we haven't changed anything
//                BasicIO.out.println("\t\t no changes to monitor queue head or its priority");
            }
        }
        else {
            // thread must be ready
//            BasicIO.out.println("\t-ready thread so reordering run queue");
            assert tm.isReady(thread):
		"Reported by: " + tm.getCurrentThread() + "=> " +
		thread + " was not ready";
            // we know tm is Ordered
            ((Ordered)tm).changeNotification(thread);
        }
    }

    /**
     * Returns the comparator that the dispatcher configured the thread
     * manager with.
     * @return the comparator that the dispatcher configured the thread
     * manager with.
     */
    public Comparator getComparator() {
        return comp;
    }

    /**
     * Sets the base priority of the primordial thread to match
     * the active priority.
     */
    protected void initPrimordialThread(OVMThread primordialThread) {
        // set active real-time priority first
        super.initPrimordialThread(primordialThread);
        // now set base priority
        if (primordialThread instanceof PriorityInheritanceOVMThreadImpl) {
            PriorityInheritanceOVMThreadImpl t = 
                (PriorityInheritanceOVMThreadImpl)primordialThread;
            int activePriority = t.getPriority();
            t.setBasePriority(activePriority);
            d("Primordial thread base and active priority set to " + 
            activePriority);
        }
    }
}
