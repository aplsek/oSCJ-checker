/*
 * SingleLinkQueue.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/s3/util/queues/SingleLinkQueue.java,v 1.12 2006/04/20 15:48:42 baker29 Exp $
 *
 */
package s3.util.queues;

import ovm.core.OVMBase;
import ovm.core.services.io.BasicIO;
/**
 * A First-In-First-Out (FIFO) queue implementation based on the use
 * of {@link SingleLinkElement SingleLinkElements}. 
 * A generic container would work with
 * Objects and wrap them in an implementation specific queue node; we
 * want to be able to avoid wrapping when it is too heavy weight and so
 * expect to work with an object that is already queue-enabled.
 *
 * <p>This class is not thread-safe. Thread safety must be enforced by the user
 * of the objects of this class.
 *
 * @author David Holmes
 */
public class SingleLinkQueue {

    protected SingleLinkElement head;
    protected SingleLinkElement tail;
    protected int size = 0;

    // no need for special constructors

    /* debug routine */
    protected void dump(BasicIO.PrintWriter out) {
        int index = 0;
        if (head == null) {
            out.println("<empty>");
        }
        else {
            out.print("Contents[size: " + size + "]: ");
            for (SingleLinkElement curr = head;
                 curr != null;
                 curr = curr.getNext() ) {
                out.print("elem["+ (index++) +"] = " + curr + ", ");
            }
            out.print("\n");
        }
    }

    /**
     * Add the specified element to the tail of this queue.
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
        size++;
        if (head == null) {
            // empty queue: assert tail==null
            head = tail = obj;
        }
        else {
            tail.setNext(obj);
            tail = obj;
            assert obj.getNext() == null : "object already linked";
        }
    }


    /**
     * Remove and return the element at the head of the queue
     *
     * @return the element at the head of the queue, or <code>null</code>
     * if the queue is empty
     *
     */
    public SingleLinkElement take() 
        throws ovm.core.services.memory.PragmaNoBarriers {
        if (head == null) {
            return null;
        }
        else {
            size--;
            SingleLinkElement temp = head;
            head = head.getNext();
            temp.setNext(null); // unlink
            if (head == null) {
                tail = null; // fix up empty queue
            }
            return temp;
        }
    }

    /**
     * Return the element at the head of the queue
     * @return the element at the head of the queue, or <code>null</code>
     * if the queue is empty
     *
     */
    public SingleLinkElement head() {
        return head;
    }

    /**
     * Return the element at the tail of the queue
     * @return the element at the tail of the queue, or <code>null</code>
     * if the queue is empty
     *
     */
    public SingleLinkElement tail() {
        return tail;
    }



    /**
     * Removes the specified object from the queue. This test is based on
     * identity (<code>==</code>) not equivalence (<code>equals()</code>).
     * @param obj the object to remove from the queue
     * @return <code>true</code> if <code>obj</code> was found in, and
     * removed from the queue, and <code>false</code> otherwise.
     *
     */
    public boolean remove(SingleLinkElement obj) {
        return findAndDelete(obj, true);
    }

    /**
     * Query whether the specified object is in the queue.
     * This test is based on
     * identity (<code>==</code>) not equivalence (<code>equals()</code>).
     * @param obj the object to look for in the queue
     * @return <code>true>/code> if <code>obj</code> is in the queue and
     * <code>false</code> otherwise.
     *
     */
    public boolean contains(SingleLinkElement obj) {
        return findAndDelete(obj, false);
    }

    /* search for and optionally delete the specified object */
    private boolean findAndDelete(SingleLinkElement obj, boolean delete) 
        throws ovm.core.services.memory.PragmaNoBarriers {
        if (obj == null || head == null) {
            return false;
        }
        // check for deleting head
        if (head == obj) {
            if (delete) {
                size--;
                head = head.getNext();
                if (head == null) {
                    tail = null; // now empty
                }
                else {
                    obj.setNext(null); // unlink
                }
            }
            return true;
        }

        for (SingleLinkElement prev = head, curr = head.getNext(); 
             curr != null;
             prev = curr, curr = curr.getNext() ) {
            if (curr == obj) {
                if (delete) {
                    size--;
                    prev.setNext(curr.getNext());
                    obj.setNext(null); // unlink
                    if (tail == obj) {
                        tail = prev;
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

    /**
     * Returns the number of elements in this queue.
     * @return the number of elements in this queue.
     */
    public int size() {
        return size;
    }
}









