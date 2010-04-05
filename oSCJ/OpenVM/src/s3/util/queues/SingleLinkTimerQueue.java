package s3.util.queues;

import ovm.core.OVMBase;
import ovm.core.services.io.BasicIO;
import s3.util.Visitor;
import ovm.core.execution.Native;
import s3.util.PragmaNoPollcheck;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.timer.TimerManager.DelayableObject;
/**
 * <p>The timer queue is like a delta queue but maintains the absolute time
 * at which elements should be removed from the queue. When the queue is
 * told to update it queries the system time and compares that against the
 * elements in the queue. All elements with times before now are removed.
 * <p>We utilise {@link SingleLinkDeltaElement} as our element types.
 *
 * <p>This class is not thread-safe. Thread safety must be enforced by the user
 * of the objects of this class. We apply PragmaNoPollcheck as an optimisation
 * as we know we're called from atomic code within the system.
 * <p>This class must be able to store references to any element so we apply
 * PragmaNoBarriers. It is up to the user to ensure such references remain
 * valid whilst in the queue. We also want to avoid the overhead of the
 * store barriers.
 *
 * @author David Holmes
 */
public class SingleLinkTimerQueue {


    // if this is true we try to use string concatenation and toString etc
    // from code that is at the heart of the threading system. So don't be
    // surprised if crashes occur.
    static final boolean DEBUG_UPDATES = false;

    /* package access for test program to see */
    SingleLinkDeltaElement head;

    /* debug routine */
    void dump(BasicIO.PrintWriter out) {
        int index = 0;
        if (head == null) {
            out.println("<empty>");
        }
        else {
            out.print("Contents: ");
            for (SingleLinkDeltaElement curr = head;
                 curr != null;
                 curr = curr.getNextDelta() ) {
                out.print("elem[");
                out.print(index++);
                out.print("] = ");
                out.print(curr);
                out.print(", ");
            }
            out.print("\n");
        }
    }

    /**
     * Create an empty timer queue.
     */
    public SingleLinkTimerQueue() {}

    /**
     * Request that the timer queue updates itself. 
     * Each object that is removed from the queue is passed to the visitor
     * for handling. The visitor must know whether to expect the actual object
     * or a {@link SingleLinkDeltaNode node} containing the object.
     * <p>Note that although each object is processed by the same visitor,
     * the objects can have different responses to having their events occur
     * if the visitor is familiar with the objects it is visiting. For example,
     * the visitor could query the object's state and act differently 
     * depending on that state.
     * @param visitor the {@link Visitor} that wants to process removed 
     * objects.
     * @return the number of objects removed
     *
     * @see Visitor
     */
    public int update(Visitor visitor) 
        throws PragmaNoPollcheck, PragmaNoBarriers {
	assert visitor != null: "null visitor for SingleLinkTimerQueue.update";

        long count = Native.getCurrentTime();
        int removals = 0;
        for (SingleLinkDeltaElement current = head; 
             current != null; 
             current = head){
            if (DEBUG_UPDATES) BasicIO.out.println("count is " + count);

            long excess = current.getDelta() - count;
            if (excess > 0) {
                if (DEBUG_UPDATES) BasicIO.out.println("excess is " + excess);
                break; // no more ready
            }
            else {
                // current is finished waiting
                if (DEBUG_UPDATES) BasicIO.out.println("head is finished");
                head = head.getNextDelta();
                current.setNextDelta(null); // unlink
                try {
                    visitor.visit(current);
                }
                catch (StackOverflowError soe) {
                    // this is fatal: just re-throw immediately
                    throw soe;
                }
                catch (Throwable t) {
                    
                    // log - but minimal - this is the guts of the threading system
                    ovm.core.execution.Native.print_string("Warning: TimerQueue visitor generated exception: ");
                    try {
                        ovm.core.execution.Native.print_string(t.toString());
                        ovm.core.execution.Native.print_string("\n");
                    }
                    catch(Throwable t2) {
                        ovm.core.execution.Native.print_string("- secondary exception trying to print original exception\n");
                    }
                }
                removals++;
            }
        }
        return removals;
    }

    /**
     * Request that the timer queue updates itself. 
     * Each object that is removed from the queue is assumed to be a 
     * <tt>DelayableObject</tt> upon which <tt>delayExpired</tt> is invoked.
     * @return the number of objects removed
     *
     * @see Visitor
     */
    public int update() 
        throws PragmaNoPollcheck, PragmaNoBarriers {
        long count = Native.getCurrentTime();
        int removals = 0;
        for (SingleLinkDeltaElement current = head; 
             current != null; 
             current = head){
            if (DEBUG_UPDATES) BasicIO.out.println("count is " + count);

            long excess = current.getDelta() - count;
            if (excess > 0) {
                if (DEBUG_UPDATES) BasicIO.out.println("excess is " + excess);
                break; // no more ready
            }
            else {
                // current is finished waiting
                if (DEBUG_UPDATES) BasicIO.out.println("head is finished");
                head = head.getNextDelta();
                current.setNextDelta(null); // unlink
                try {
                    ((DelayableObject)current).delayExpired();
                }
                catch (StackOverflowError soe) {
                    // this is fatal: just re-throw immediately
                    throw soe;
                }
                catch (Throwable t) {
                    
                    // log - but minimal - this is the guts of the threading system
                    ovm.core.execution.Native.print_string("Warning: TimerQueue visitor generated exception: ");
                    try {
                        ovm.core.execution.Native.print_string(t.toString());
                        ovm.core.execution.Native.print_string("\n");
                    }
                    catch(Throwable t2) {
                        ovm.core.execution.Native.print_string("- secondary exception trying to print original exception\n");
                    }
                }
                removals++;
            }
        }
        return removals;
    }




    /**
     * Inserts the given object into the timer queue, such that it will
     * remain there until the given time has passed. If the time has
     * already passed then the object is not inserted and we return
     * <code>false</code>.
     * <p>If the given release time is equal to that of an element already
     * in the queue then the new element is inserted behind it.
     *
     * @param obj the object to insert
     * @param releaseTime the time at which to release the element, measured
     * in nanoseconds since the epoch.
     * @return <code>true</code> if the object is inserted, and 
     * <code>false</code> if the specified release time has already passed.
     */
    public boolean insert (SingleLinkDeltaElement obj, long releaseTime)
        throws PragmaNoPollcheck, PragmaNoBarriers {
	assert obj.getNextDelta() == null:
	    "dangling reference on inserted node";
//            BasicIO.out.println("Release time = " + releaseTime + "\n" +
//                                "Current time = " + Native.getCurrentTime());
        // ensure we set the delta field
        obj.setDelta(releaseTime);
        if (releaseTime <= Native.getCurrentTime()) {
            return false;
        }

        // NOTE: when inserting an object with the same release time as an
        //       existing entry we must place the new entry behind the existing
        //       one. We want this for maintaining priority ordering.

        // empty queue?
        if (head == null) {
            head = obj;
        }
        else if (head.getNextDelta() == null) {
            // one element queue
            if (releaseTime < head.getDelta()) {
                // insert at head
                obj.setNextDelta(head);
                head = obj;
            }
            else {
                // insert at tail
                head.setNextDelta(obj);
            }
        } else { // at least two elements
            // check for head insertion
            if (releaseTime < head.getDelta()) {
                obj.setNextDelta(head);
                head = obj;
            }
            else { // step through
                SingleLinkDeltaElement prev = null, curr = null;
                for (prev = head, curr = head.getNextDelta();
                     curr != null;
                     prev = curr, curr = curr.getNextDelta() ) {
                    long currTime = curr.getDelta();
                    if (releaseTime < currTime) {
                        // insert before curr
                        obj.setNextDelta(curr);
                        prev.setNextDelta(obj);
                        return true;
                    }
                }
                // if we get here we go at the end
                prev.setNextDelta(obj);
            }
        }
        return true;
    }                        

    /**
     * Removes the specified object from the queue. This test is based on
     * identity (<code>==</code>) not equivalence (<code>equals()</code>).
     * @param obj the object to remove from the queue
     * @return <code>true</code> if <code>obj</code> was found in, and
     * removed from the queue, and <code>false</code> otherwise.
     *
     */
    public boolean remove(SingleLinkDeltaElement obj) 
        throws PragmaNoPollcheck {
        return findAndDelete(obj, true);
    }

    /**
     * Query whether the specified object is in the queue.
     * This test is based on
     * identity (<code>==</code>) not equivalence (<code>equals()</code>).
     * @param obj the object to look for in the queue
     * @return <code>true</code> if <code>obj</code> is in the queue and
     * <code>false</code> otherwise.
     *
     */
    public boolean contains(SingleLinkDeltaElement obj) 
        throws PragmaNoPollcheck{
        return findAndDelete(obj, false);
    }

    /* search for and optionally delete the specified object */
    private boolean findAndDelete(SingleLinkDeltaElement obj, boolean delete) 
        throws PragmaNoPollcheck, PragmaNoBarriers {
        if (obj == null || head == null) {
            return false;
        }
        // check for deleting head
        if (head == obj) {
            if (delete) {
                head = head.getNextDelta();
                obj.setNextDelta(null); // unlink
            }
            return true;
        }

        for (SingleLinkDeltaElement prev = head, curr = head.getNextDelta(); 
             curr != null;
             prev = curr, curr = curr.getNextDelta() ) {
            if (curr == obj) {
                if (delete) {
                    prev.setNextDelta(curr.getNextDelta());
                    curr = curr.getNextDelta();
                    obj.setNextDelta(null); // unlink
                }
                return true;
            }
        }
        
        return false;
    }


    /**
     * Queries whether there are any elements in this queue
     * @return <code>true</code> if there are no elements in this queue
     * and <code>false</code> otherwise.
     *
     */
    public boolean isEmpty() throws PragmaNoPollcheck {
        return (head == null);
    }

}










