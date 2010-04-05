package s3.util.queues;

import ovm.core.services.io.BasicIO;
import s3.util.Visitor;
import ovm.util.OVMError;
import ovm.core.OVMBase;
import ovm.core.execution.Native;
import s3.util.PragmaNoPollcheck;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.timer.TimerManager.DelayableObject;
/**
 * An implementation of a delta queue based on the use of 
 * {@link SingleLinkDeltaElement SingleLinkDeltaElements}. 
 * A delta queue is a special type of queue which allows objects to stay in the
 * queue until a specified number of events have occurred. Upon an 
 * {@link #update update} each object in the queue is examined to see if the
 * number of events it was waiting for have now occurred. If they have then 
 * the object is removed from the queue and &quot;visited&quot;. Visiting
 * is effected using two methods: either update is called with a visitor to
 * apply to all elements, or update is called with no visitor in which case
 * the elements are assumed to be <tt>DelayableObject</tt> instances, upon
 * which <tt>delayExpired</tt> is invoked.
 *
 * <p>To avoid having to examine every object on each update objects are 
 * ordered
 * according to the difference in the number of events being waited upon by
 * this object and by its predecessor - its &quot;delta&quot; value.
 * The object at the head of the queue is
 * the one waiting for the fewest number of events, while the object at the
 * tail is waiting for the most - the actual number of events being waited
 * upon by any object is the sum of the values stored from the head object to
 * that object.
 * <p>Commonly the events being waited upon are timer ticks and the values are
 * simply the number of ticks that the object must stay in the queue. This is
 * typically used for maintaining &quot;sleep&quot; queues.
 * <p>The interface allows searching of and
 * removing of arbitrary elements in the queue.
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
public class SingleLinkDeltaQueue {

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
     * Create an empty delta queue.
     */
    public SingleLinkDeltaQueue() {}

    /**
     * Updates the delta queue using the number of events specified. Each
     * object that is removed from the queue is passed to the visitor
     * for handling. The visitor must know whether to expect the actual object
     * or a {@link SingleLinkDeltaNode node} containing the object.
     * <p>Note that although each object is processed by the same visitor,
     * the objects can have different responses to having their events occur
     * if the visitor is familiar with the objects it is visiting. For example,
     * the visitor could query the object's state and act differently 
     * depending on that state.
     * @param count the number of events that have passed since the last update
     * @param visitor the {@link Visitor} that wants to process removed 
     * objects. If <code>visitor</code> is <code>null</code> then the object is
     * just removed.
     * @return the number of objects removed
     * @throws OVMError.IllegalArgument if count is less than one
     *
     * @see Visitor
     */
    public int update(long count, Visitor visitor) 
        throws PragmaNoPollcheck, PragmaNoBarriers {
        if (count < 1) {
            throw new OVMError.IllegalArgument("illegal count " + count + 
                                               " must be >0");
        }
	assert visitor != null: "null visitor for SingleLinkDeltaQueue.update";

        int removals = 0;
        for (SingleLinkDeltaElement current = head; 
             current != null; 
             current = head){
            if (DEBUG_UPDATES) BasicIO.out.println("count is " + count);
            long excess = head.subtractDelta(count);
            if (excess > 0) {
                if (DEBUG_UPDATES) BasicIO.out.println("excess is " + excess);
                break; // no more ready
            }
            else {  // current is finished
                if (DEBUG_UPDATES) BasicIO.out.println("head is finished");
                head = head.getNextDelta();
                current.setNextDelta(null); // unlink
                current.setDelta(0); // make sure non-negative as this is a 
                                     // a flag used by JLThread
                try {
                    visitor.visit(current);
                }
                catch (StackOverflowError soe) {
                    // this is fatal: just throw immediately
                    throw soe;
                }
                catch (Throwable t) {
                    // log - but minimal - this is the guts of the threading system
                    Native.print_string("Warning: Delta Queue visitor generated exception: ");
                    try {
                        Native.print_string(t.toString());
                        Native.print_string("\n");
                    }
                    catch(Throwable t2) {
                        Native.print_string("- secondary exception trying to print original exception\n");
                    }
                }
            }
            if (excess < 0) {
                count = -excess;
            }
            removals++;
        }
        return removals;
    }

    /**
     * Updates the delta queue using the number of events specified. Each
     * object that is removed from the queue is assumed to be a 
     * <tt>DelayableObject</tt> upon which <tt>delayExpired</tt> is invoked.
     * @param count the number of events that have passed since the last update
     * @return the number of objects removed
     * @throws OVMError.IllegalArgument if count is less than one
     *
     * @see Visitor
     */
    public int update(long count) 
        throws PragmaNoPollcheck, PragmaNoBarriers {
        if (count < 1) {
            throw new OVMError.IllegalArgument("illegal count " + count + 
                                               " must be >0");
        }
        int removals = 0;
        for (SingleLinkDeltaElement current = head; 
             current != null; 
             current = head){
            if (DEBUG_UPDATES) BasicIO.out.println("count is " + count);
            long excess = head.subtractDelta(count);
            if (excess > 0) {
                if (DEBUG_UPDATES) BasicIO.out.println("excess is " + excess);
                break; // no more ready
            }
            else {  // current is finished
                if (DEBUG_UPDATES) BasicIO.out.println("head is finished");
                head = head.getNextDelta();
                current.setNextDelta(null); // unlink
                current.setDelta(0); // make sure non-negative as this is a 
                                     // a flag used by JLThread
                try {
                    ((DelayableObject)current).delayExpired();
                }
                catch (StackOverflowError soe) {
                    // this is fatal: just throw immediately
                    throw soe;
                }
                catch (Throwable t) {
                    // log - but minimal - this is the guts of the threading system
                    Native.print_string("Warning: Delta Queue visitor generated exception: ");
                    try {
                        Native.print_string(t.toString());
                        Native.print_string("\n");
                    }
                    catch(Throwable t2) {
                        Native.print_string("- secondary exception trying to print original exception\n");
                    }
                }
            }
            if (excess < 0) {
                count = -excess;
            }
            removals++;
        }
        return removals;
    }


    /**
     * Inserts the given object into the delta queue, such that it will
     * remain there until <code>count</code> events have passed.
     *
     * @param obj the object to insert
     * @param count the number of events to wait for
     * @throws OVMError.IllegalArgument if <code>count</code> is less than one
     */
    public void insert (SingleLinkDeltaElement obj, long count) 
        throws PragmaNoBarriers {
        if (count < 1) {
            throw new OVMError.IllegalArgument("Count must be > 0");
        }

	assert obj.getNextDelta() == null:
	    "dangling reference on inserted node";

        // NOTE: when inserting an object with the same delta as an
        //       existing entry we must place the new entry behind the existing
        //       one. We want this for maintaining priority ordering.


        // empty queue?
        if (head == null) {
            head = obj;
            obj.setDelta(count);
        }
        else if (head.getNextDelta() == null) {
            // one element queue
            if (count < head.getDelta()) {
                // insert at head
                head.subtractDelta(count);
                obj.setNextDelta(head);
                head = obj;
                obj.setDelta(count);
            }
            else {
                // insert at tail
                obj.setDelta(count - head.getDelta());
                head.setNextDelta(obj);
            }
        } else { // at least two elements
            // check for head insertion
            if (count < head.getDelta()) {
                head.subtractDelta(count);
                obj.setNextDelta(head);
                head = obj;
                obj.setDelta(count);
            }
            else { // step through
                count -= head.getDelta();
                SingleLinkDeltaElement prev = null, curr = null;
                for (prev = head, curr = head.getNextDelta();
                     curr != null;
                     prev = curr, curr = curr.getNextDelta() ) {
                    long currDelta = curr.getDelta();
                    if (count < currDelta) {
                        // insert before curr
                        curr.subtractDelta(count);
                        obj.setNextDelta(curr);
                        prev.setNextDelta(obj);
                        obj.setDelta(count);
                        return;
                    }
                    count -= currDelta;
                }
                // if we get here we go at the end
                obj.setDelta(count);
                prev.setNextDelta(obj);
                obj.setNextDelta(null); // safety precaution
            }
        }
    }                        

    /**
     * Removes the specified object from the queue. This test is based on
     * identity (<code>==</code>) not equivalence (<code>equals()</code>).
     * @param obj the object to remove from the queue
     * @return <code>true</code> if <code>obj</code> was found in, and
     * removed from the queue, and <code>false</code> otherwise.
     *
     */
    public boolean remove(SingleLinkDeltaElement obj) {
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
    public boolean contains(SingleLinkDeltaElement obj) {
        return findAndDelete(obj, false);
    }

    /* search for and optionally delete the specified object */
    private boolean findAndDelete(SingleLinkDeltaElement obj, boolean delete) 
        throws ovm.core.services.memory.PragmaNoBarriers {
        if (obj == null || head == null) {
            return false;
        }
        // check for deleting head
        if (head == obj) {
            if (delete) {
                head = head.getNextDelta();
                if (head != null) { // fix up delta for next element
                    head.addDelta(obj.getDelta());
                }
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
                    if (curr != null) {
                        curr.addDelta(obj.getDelta()); // fix up delta
                    }
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
    public boolean isEmpty() {
        return (head == null);
    }

}










