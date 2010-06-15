/*
 * SingleLinkPriorityQueue.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/s3/util/queues/SingleLinkPriorityQueue.java,v 1.14 2006/04/20 15:48:42 baker29 Exp $
 */
package s3.util.queues;

import java.util.Comparator;

import ovm.core.OVMBase;

/**
 * A priority queue based on the use of {@link SingleLinkElement SingleLinkElements}. This
 * class extends {@link SingleLinkQueue} by allowing the order of insertion to
 * be defined via a {@link java.util.Comparator comparator}. In addition we add
 * methods useful for qworking with multiple queues.
 *
 * <p>This class is not thread-safe. Thread safety must be enforced by the user
 * of the objects of this class.
 *
 * @author David Holmes
 */
public class SingleLinkPriorityQueue 
    extends SingleLinkQueue 
    implements ovm.util.Ordered {

    protected Comparator comp;

    /**
     * Constructs a priority queue with an, as yet, unspecified ordering.
     * @see #setComparator
     *
     */
    public SingleLinkPriorityQueue() {}

    /**
     * Constructs a priority queue using the given comparator to control
     * insertion order.
     *
     * @param comp the comparator that will control insertion order
     * @throws IllegalArgumentException if <code>comp</code> is null
     *
     */
    public SingleLinkPriorityQueue(Comparator comp) {
        if (comp == null) {
            throw new IllegalArgumentException("comparator was null");
        }
        this.comp = comp;
    }

    /**
     * Sets the comparator to be used to order this priority queue.
     * The comparator can only be set once, either here, or via the
     * constructor.
     *
     * @throws IllegalStateException if the comparator has already been set
     * @throws IllegalArgumentException if <code>comp</code> is <code>null</code>
     *
     */
    public void setComparator(Comparator comp) {
        if (this.comp != null) {
            throw new IllegalStateException("comparator already set");
        }
        if (comp == null) {
            throw new IllegalArgumentException("comparator was null");
        }
        this.comp = comp;
    }

    public Comparator getComparator() {
        return this.comp;
    }


    // an elements priority got changed so fix the queue
    public void changeNotification(Object o) {
        SingleLinkElement obj = (SingleLinkElement) o;
        // take it out and put it back in right
        if (remove(obj)) {
            add(obj);
        }
    }


    /**
     * Add the specified element to the queue. The element will be inserted
     * at the position determined by the comparator for this queue. Elements
     * of equal priority, according to the comparator, are added FIFO.
     *
     * @param obj the object to add to the queue
     *
     * @throws IllegalArgumentException is <code>obj</code> is <code>
     * null</code>
     *
     */
    public void add(SingleLinkElement obj) 
        throws ovm.core.services.memory.PragmaNoBarriers {
        if (obj == null) {
            throw new IllegalArgumentException("adding null element");
        }
        assert obj.getNext() == null : "object already linked";

        size++; // we will add the object

        if (head == null) {
            // empty queue: 
            assert tail==null : "tail not null when head null";
            head = tail = obj;
        }
        else {// find the place to insert
            // check for new head
            if (comp.compare(head, obj) < 0) {
                obj.setNext(head);
                head = obj;
                return;
            }
            SingleLinkElement prev = null, curr = null;
            for (prev = head, curr = head.getNext(); 
                 curr != null; 
                 prev = curr, curr = curr.getNext()) {
                // we insert before the first element with lower priority
                // so equal priority items get inserted FIFO
                if (comp.compare(curr,obj) < 0) {
                    // insert before
                    obj.setNext(curr);
                    prev.setNext(obj);
                    return;
                }
            }
            // came to end of list so insert at tail
            tail.setNext(obj);
            tail = obj;
        }
    }

    /**
     * Merges all elements of the given priority queue into the current one,
     * emptying the given queue in the process. This method is a more optimal
     * way of transferring all elements from one priority queue to another
     * (such as when a <code>notifyAll</code> requires transfer of all waiting
     * threads to the monitor queue), without having to pull the queue apart
     * one element at a time and insert one element at a time. Of course, this
     * optimisation can only occur if the two priority queues use the same
     * comparator, otherwise we must do an element by element insertion.
     * <p><b>Note:</b> when a merged element has the same priority as an existing
     * element, it will be inserted after that element. There is no way to maintain
     * FIFO in any time-sense because the time of entry to queues is not tracked.
     *
     * @param other the queue to be merged (null is permitted but does nothing)
     *
     * @throws IllegalArgumentException if an attempt is made to merge a queue
     * with itself.
     *
     */
    public void merge(SingleLinkPriorityQueue other) 
        throws ovm.core.services.memory.PragmaNoBarriers {
        //sanity check first
        if (other == this) {
            throw new IllegalArgumentException("attempt to merge queue with itself");
        }

        // check special cases

        if ( other == null || other.isEmpty() ) {
             // nothing to do
        } else if (head == null) {  // are we empty?
            head = other.head;
	    tail = other.tail;
            size = other.size;
            other.head = other.tail = null;
            other.size = 0;
        } else if (other.head == other.tail) {  // only one element in other ?
            this.add(other.head);
            other.head = other.tail = null;
            other.size = 0;
        } else if (comp.equals(other.comp)) {     // do we use the same comparators
            // if we use the same comparators and the other queue has more
            // than one element then we must iteratore through, but we can
            // check for a few more special cases like append/prepend.
            // In all cases the end result is that:
            //  this.size == original this.size + other.size
            //  other.size == 0
            // so we do that up front
            size += other.size;
            other.size = 0;

            if (comp.compare(other.tail, head) > 0 ) {
                // strictly greater so prepend other to our head
                other.tail.setNext(head); // link the two queues
                head = other.head;
                other.head = other.tail = null;
            }
            else if (comp.compare(tail, other.head) >= 0 ) {
                // strictly less than so append other to our tail
                tail.setNext(other.head);
                tail = other.tail;
                other.head = other.tail = null;
            } else if ( head == tail) {  // we only have one element?
                // note we can't simply do other.add(head) because that 
                // violates the ordering requirement that our elements 
                // appear before those of the other queue when priority 
                // is the same

                // we know that other has >1 element so find the right 
                // place to insert our element in front of other's of the 
                // same priority. We know we don't go at head of
                // other because we already tested that above
                for (SingleLinkElement otherprev = other.head,
                         othercurr = other.head.getNext();
                     othercurr != null;
                     otherprev = othercurr, othercurr = othercurr.getNext() ) {
//                    System.out.print("Comparing: " + 
//                                       ((PQDriver.TestNode)head).val + ", " + ((PQDriver.TestNode)othercurr).val);

                    if (comp.compare(head, othercurr) >= 0) {
//                        System.out.println(" result is >=0");
                        // insert before othercurr
                        head.setNext(othercurr);
                        otherprev.setNext(head);
                        // now switch head & tail
                        head = other.head;
                        tail = other.tail;
                        other.head = other.tail = null;
                        return;
                    } else {
//                        System.out.println(" result is <0");
                    }
                }
                // if we get here it should mean insert as new tail of other, 
                // but that case has already been checked with the strictly 
                // greater/less checks so we should never get here.
                assert false : "strictly less check failed";

            } else { // need complete merge, element by elemenbt
                // we need only iterate once through the current
                // queue looking for the right insertion spots for all
                // of the elements in other. This is only possible
                // because other is sorted the same way that we are
                SingleLinkElement othercurr = other.head;
outer:                for(SingleLinkElement prev = null, 
                        curr = head;
                    curr != null && othercurr != null;
                    prev = curr,curr = curr.getNext() ) {

                    if (comp.compare(curr, othercurr) < 0) {
                        // at least othercurr gets inserted before
                        // but there could be other elements in other
                        // that should also go before curr, so we look
                        // for them now

                        SingleLinkElement othernext = othercurr.getNext();
                        if (othernext == null) { // only othercurr to do
                            othercurr.setNext(curr);
                            if (prev != null) {
                                prev.setNext(othercurr);
                            }
                            else { // new head
                                head = othercurr;
                            }
                            othercurr = null; // we're done
                        }
                        else {
                            SingleLinkElement otherprev = othercurr;
                            for ( /* already inited */;
                                  othernext != null;
                                  otherprev = othernext, othernext = othernext.getNext() ) {
                                if (comp.compare(curr, othernext) < 0) {
                                    continue;
                                }
                                else {
                                    // insert othercurr to otherprev
                                    otherprev.setNext(curr);
                                    if (prev != null) {
                                        prev.setNext(othercurr);
                                    }
                                    else {// new head
                                        head = othercurr;
                                    }
                                    othercurr = othernext;
                                    continue outer;
                                }
                            }
                            // ran off the end so insert remaining before curr
                            if ( prev != null) {
                                prev.setNext(othercurr);
                            }
                            else { // new head
                                head = othercurr;
                            }
                            assert other.tail == otherprev : "othertail != otherprev";
                            other.tail.setNext(curr);
                            othercurr = null; // all done
                        }
                    }
                }
                // we might have come to our end before inserting all 
                // elements of other so the rest are appended at our tail
                if (othercurr != null) {
                    tail.setNext(othercurr);
                    tail = other.tail;
                }
                // clear other
                other.head = other.tail = null;
            }
        }
        else { // different comparators so do it element by element : no choice
            // note: size is dealt with by add/take
            while (!other.isEmpty()) {
                add(other.take());
            }
        }
    }

   
}









