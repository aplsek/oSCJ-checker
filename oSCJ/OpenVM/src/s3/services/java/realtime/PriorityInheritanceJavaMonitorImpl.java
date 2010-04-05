package s3.services.java.realtime;
import java.util.Comparator;

import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.threads.OVMThread;
import ovm.services.java.JavaMonitor;
import ovm.services.java.JavaOVMThread;
import ovm.services.java.realtime.RealtimeJavaMonitor;
import ovm.services.monitors.Monitor;
import ovm.services.monitors.TransferableMonitor;
import ovm.util.OVMError;
import s3.core.domain.S3Domain;
import s3.services.java.ulv1.JavaMonitorImpl;
import s3.services.java.ulv1.JavaOVMThreadImpl;
import s3.util.PragmaNoPollcheck;
import s3.util.queues.SingleLinkPriorityQueue;
/**
 * An extension of the {@link JavaMonitorImpl} monitor class to support
 * the Priority Inheritance Protocol implementation.
 * This implementation accounts for
 * the arbitrary change of a thread's priority programmatically at runtime.
 *
 * @author David Holmes
 *
 * @see <a href="{@docRoot}/s3/services/realtime/doc-files/PriorityInheritance.html">
 * Priority Inheritance Protocol Implementation</a>
 */
public class PriorityInheritanceJavaMonitorImpl extends JavaMonitorImpl 
    implements RealtimeJavaMonitor, TransferableMonitor { 

    protected RealtimeJavaDispatcherImpl dispatcher =  (RealtimeJavaDispatcherImpl) super.dispatcher;

    /**
     * Construct a monitor using the default comparator as configured in
     * the current thread manager.
     */
    protected PriorityInheritanceJavaMonitorImpl() {
        super();
//        DEBUG = true;
    }

    /**
     * Construct a monitor using the supplied comparator for maintaining
     * the entry and wait queues.
     */
    protected PriorityInheritanceJavaMonitorImpl(Comparator comp) {
        super(comp);
    }


    /** The factory object */
    public static final Monitor.Factory factory = new Factory();

    /**
     * The factory class for creating monitors 
     */
    public static class Factory extends JavaMonitorImpl.Factory {
        public Monitor newInstance() { return new PriorityInheritanceJavaMonitorImpl(); }
        public JavaMonitor newJavaMonitorInstance() { return new PriorityInheritanceJavaMonitorImpl(); }
        public int monitorSize() { return PriorityInheritanceJavaMonitorImpl.sizeOf(); }
    }


    /**
     *  Returns the actual size of an instance of this class, including the
     *  space needed for the object header and all fields, plus the space
     *  needed for creating referenced objects (and transitively the space
     *  they need to create referenced objects) during construction.
     */
    static int sizeOf() {
        return 
            S3Domain.sizeOfInstance("s3/services/java/realtime/PriorityInheritanceJavaMonitorImpl")
            + constructionSizeOf();
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
        // there is no additional allocation in this class so just return
        // whatever our super class construction requirements are
        return JavaMonitorImpl.constructionSizeOf();
    }

    // override the customization hooks to effect the BPIP

    /**
     * Inserts the current thread into the inheritance set of the thread 
     * owning this monitor and ensures all priority releationships are
     * maintained. Ideally this would only be necessary if the current thread's
     * priority were greater than the owner, but the priorities of either 
     * thread could change at any time.
     */
    protected boolean onBlocking(OVMThread current) throws PragmaNoPollcheck {
        // Test me: if there is a deadlock situation it is not clear what will
        // happen - so we need to test this.
	if (GATHER_MONITOR_STATISTICS)M_BLOCK++;
//	BasicIO.out.println("Block " + current);
//	new Error().printStackTrace();	

	
	
//new Exception().printStackTrace();		 
        // when called the thread is not yet in the entryQ.
	RealtimeJavaThreadImpl head = (RealtimeJavaThreadImpl) entryQ.head();
	RealtimeJavaThreadImpl owner_ = (RealtimeJavaThreadImpl) this.owner;

	if (head == null) {
	    if (DEBUG) BasicIO.out.println("\t\tno current waiters in entryQ so " 
		    + "adding current to owners (" + owner_.getName() + ") inheritance queue");
	    owner_.inheritanceQueue.add(((RealtimeJavaThreadImpl) current).node);
	    if (DEBUG) BasicIO.out.println("\t\tcalling maintainPriorityRelations");	    
	    dispatcher.maintainPriorityRelations(owner_);
	} else if (comp.compare(current, head) > 0) {
	    if (DEBUG) BasicIO.out.println("\t\tcurrent has higher priority than " + "entryQ head (" + head.getName()
		    + ") so removing head and adding current " + "to owners (" + owner_.getName()
		    + ") inheritance queue");
	    
	    owner_.inheritanceQueue.remove(head.node);
	    owner_.inheritanceQueue.add(((RealtimeJavaThreadImpl) current).node);	    
	    if (DEBUG) BasicIO.out.println("\t\tcalling maintainPriorityRelations");	    
	    dispatcher.maintainPriorityRelations(owner_);
	} else // else - nothing to do
	    if (DEBUG) BasicIO.out.println("\t\tcurrent has lower priority than " + "entryQ head (" 
		    + head.getName() + ") so doing nothing");
	return true;
    }


    /**
     * The owner thread has just acquired this monitor so if there are
     * any waiters then the head waiter must be added to the inheritance
     * queue of the owner.
     */
    protected void onAcquire() throws PragmaNoPollcheck {
	RealtimeJavaThreadImpl head = (RealtimeJavaThreadImpl) entryQ.head();
	RealtimeJavaThreadImpl owner_ = (RealtimeJavaThreadImpl) this.owner;

	if (head == null) {
	    if (DEBUG) BasicIO.out.println("\t\tno current waiters in entryQ so nothing to do");
	} else {
	    if (DEBUG) BasicIO.out.println("\t\t adding entryQ head (" + head.getName() + " to owners ("
		    + owner_.getName() + ") inheritance queue");
	    
	    owner_.inheritanceQueue.add(head.node);
	    
	    if (DEBUG) BasicIO.out.println("\t\tcalling maintainPriorityRelations");
	    
	    dispatcher.maintainPriorityRelations(owner_);
	}
    }

    /**
     * Inserts the given thread into the inheritance set of the thread 
     * owning this monitor and ensures all priority releationships are
     * maintained. Ideally this would only be necessary if the current thread's
     * priority were greater than the owner, but the priorities of either 
     * thread could change at any time.
     */
    protected void onBlockingAfterWait(OVMThread t) throws PragmaNoPollcheck {
        // Test me: if there is a deadlock situation it is not clear what will
        // happen - so we need to test this.

        // when called the thread is not yet in the entryQ.
        RealtimeJavaThreadImpl head = (RealtimeJavaThreadImpl) entryQ.head();
	RealtimeJavaThreadImpl owner_ = (RealtimeJavaThreadImpl) this.owner;

	if (head == null) {
	    if (DEBUG) BasicIO.out.println("\t\tno current waiters in entryQ so " 
		    + "adding t to owners ("  + owner_.getName() + ") inheritance queue");

	    owner_.inheritanceQueue.add(((RealtimeJavaThreadImpl) t).node);
	    if (DEBUG) BasicIO.out.println("\t\tcalling maintainPriorityRelations");
	    dispatcher.maintainPriorityRelations(owner_);
	} else if (comp.compare(t, head) > 0) {
	    if (DEBUG) BasicIO.out.println("\t\tt has higher priority than " + "entryQ head (" 
		    + head.getName()+") so removing head and adding t "+"to owners ("+owner_.getName() + ") inheritance queue");
	    owner_.inheritanceQueue.remove(head.node);
	    owner_.inheritanceQueue.add(((RealtimeJavaThreadImpl) t).node);
	    if (DEBUG) BasicIO.out.println("\t\tcalling maintainPriorityRelations");
	    dispatcher.maintainPriorityRelations(owner_);
	} else if (DEBUG) BasicIO.out.println("\t\t t has lower priority than " 
		+ "entryQ head (" + head.getName() + ") so doing nothing");
    }


    /**
     * Clears the inheritance queue of the current thread of the reference to
     * the head locker of this monitor.
     */
    protected void onExit() throws PragmaNoPollcheck {
        // note: this.owner = currentThread
        RealtimeJavaThreadImpl owner_ = (RealtimeJavaThreadImpl) this.owner;
        RealtimeJavaThreadImpl head = (RealtimeJavaThreadImpl)entryQ.head();
        if (head != null) {
            if (DEBUG) BasicIO.out.println("\t\t removing " + "entryQ head (" + head.getName() 
        	    	                + "from owners (" + owner_.getName() + ") inheritance queue");

            owner_.inheritanceQueue.remove(head.node);
            dispatcher.maintainPriorityRelations(owner_);
        }
    }

        

    // implement the absolute timed wait

    /**
     * @param ignored - the monitor and condition queue are the same object
     * so no monitor needs to be passed
     */
    public int waitAbsoluteTimedAbortable(Monitor ignored, long deadline) {
	JavaOVMThreadImpl current = (JavaOVMThreadImpl) jtm.getCurrentThread();
	if (DEBUG) BasicIO.out.println("Monitor waitAbsoluteTimedAbortable by thread " + current.getName());
	checkOwner(current);

	// need atomicity
	boolean enabled = jtm.setReschedulingEnabled(false);
	try {
	    // watch for an already interrupted Java thread
	    if (current.isInterrupted()) {
		if (DEBUG) BasicIO.out.println("\tinterrupted on entry: aborting");
		return ABORTED;
	    }
	    current.setVisitor(timeoutAction);
	    if (!timer.delayAbsolute(current, deadline)) {
		// time already passed so just return
		if (DEBUG) BasicIO.out.println("\tdeadline passed already");
		return DEADLINE_PASSED;
	    }
	    if (DEBUG) BasicIO.out.println("\tadded to abs delay queue");

            int count = entryCount;
            entryCount = 1; // ready for doExit()
            jtm.removeReady(current);
            waitQ.add(current);
            waitSize++;
            if (DEBUG) BasicIO.out.println("\tadded to wait queue");
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


    protected void removeFromTimedWaitQueue(JavaOVMThreadImpl thread) {
        if (DEBUG) BasicIO.out.println("\ttry removing from abs delay queue");
        if (!timer.wakeUpAbsolute(thread)) {
            // not an absolute wait? Try relative by invoking super()
            super.removeFromTimedWaitQueue(thread);
        }
        else if (DEBUG) BasicIO.out.println("\tremoved from abs delay queue");
    }


    /**
     * Expose our entry queue so the dispatcher can implement the PIP
     * correctly.
     */
    // public for the test programs
     public SingleLinkPriorityQueue getEntryQueue() { return entryQ; } 


    /**
     * Transfer ownership of this monitor from the current thread to the
     * given thread. The current thread must not hold this monitor
     * recursively, and the new owner must not already be in the entry queue
     * of, or waiting on, this monitor.
     *
     * @param to the thread to transfer ownship to
     *
     * @throws OVMError.IllegalState if new owner is waiting on or entering 
     * this monitor; or the current thread holds this monitor recursively
     * @throws IllegalMonitorStateException if the current thread doesn't
     * own this monitor
     */
    public void transfer(OVMThread to)   throws PragmaNoBarriers, PragmaNoPollcheck {
        RealtimeJavaThreadImpl newOwner = (RealtimeJavaThreadImpl) to;
	if (DEBUG) BasicIO.out.println("PriorityInheritanceJavaMonitorImpl transfer");
	
	RealtimeJavaThreadImpl current = (RealtimeJavaThreadImpl) jtm.getCurrentThread();
	if (DEBUG) BasicIO.out.println("Monitor transfer by thread " + current.getName());
	
	checkOwner(current);
	if (entryCount > 1) throw new OVMError.IllegalState("recursively held monitor");
	
	// check newOwner not in entryQ or waitQ
	if (newOwner.getWaitingMonitor() == this || newOwner.getWaitingConditionQueue() == this) 
	    throw new OVMError.IllegalState("newOwner blocked on monitor");
	// need atomicity for PIP maintenance
	boolean enabled = jtm.setReschedulingEnabled(false);
	try {
	    // first remove this monitor as a PIP source for the current thread
	    RealtimeJavaThreadImpl head = (RealtimeJavaThreadImpl) entryQ.head();
	    if (head != null) {
		current.inheritanceQueue.remove(head.node);
		dispatcher.maintainPriorityRelations(current);
	    }
	    // install new owner
	    owner = newOwner;
	    // now add this monitor as a PIP source for the new owner
	    if (head != null) {
		newOwner.inheritanceQueue.add(head.node);
		dispatcher.maintainPriorityRelations(newOwner);
	    }
	    if (DEBUG) BasicIO.out.println("Monitor transfer by thread " + current.getName() + " to "
		    + newOwner.getName());
	} finally {
	    jtm.setReschedulingEnabled(enabled);
	}
    }

}




