/*
 * SingleLinkDeltaNode.java
 *
 * Created 6 December 2001, 15:30
 *
 * $Header: /p/sss/cvs/OpenVM/src/s3/util/queues/SingleLinkDeltaNode.java,v 1.6 2004/02/20 08:53:15 jthomas Exp $
 */
package s3.util.queues;


/**
 * A wrapper object that can form part of a singly-linked delta list.
 *
 * @see SingleLinkDeltaQueue
 *
 * @author David Holmes
 *
 */
public class SingleLinkDeltaNode implements SingleLinkDeltaElement {

    private SingleLinkDeltaElement next;
    private Object value;
    private long delta;

    /**
     * Create a node that is unlinked, refers to no object and has a delta
     * value of zero.
     * That is, after construction 
     * <code>this.getNext() == next && this.getValue() == null && 
     * this.getDelta() == 0</code>.

     *
     */
    public SingleLinkDeltaNode() {
        this(null, null, 0);
    }

    /**
     * Create a node that is linked to the given element, refers to no
     * object and has a delta value of zero. 
     * That is, after construction 
     * <code>this.getNext() == next && this.getValue() == null && 
     * this.getDelta() == 0</code>.
     *
     * @param next the element to which this node should be linked
     *
     */
    public SingleLinkDeltaNode(SingleLinkDeltaElement next) {
        this(next, null, 0);
    }

    /**
     * Create an unlinked node that refers to the given object and with a
     * delta value of zero. 
     * That is, after construction 
     * <code>this.getNext() == null && this.getValue() == value && 
     * this.getDelta() == 0</code>.
     *
     * @param value the object to which this node should refer
     *
     */
    public SingleLinkDeltaNode(Object value) {
        this(null, value, 0);
    }

    /**
     * Create a node that is linked to the given element, refers to the
     * given object and has a delta value of zero.
     * That is, after construction 
     * <code>this.getNext() == next && this.getValue() == value && 
     * this.getDelta() == 0</code>.

     * @param next the element to which this node should be linked
     * @param value the object to which this node should refer
     *
     */
    public SingleLinkDeltaNode(SingleLinkDeltaElement next, Object value) {
        this(next, value, 0);
    }

    /**
     * Create a node that is linked to the given element, refers to the
     * given object and has the given delta value.
     * That is, after construction 
     * <code>this.getNext() == next && this.getValue() == value && 
     * this.getDelta() == delta</code>.

     * @param next the element to which this node should be linked
     * @param value the object to which this node should refer
     * @param delta the initial delta value
     *
     */
    public SingleLinkDeltaNode(SingleLinkDeltaElement next, Object value, long delta) {
        this.next = next;
        this.value = value;
        this.delta = delta;
    }

    public void setNextDelta(SingleLinkDeltaElement next) {
        this.next = next;
    }

    public SingleLinkDeltaElement getNextDelta() {
        return this.next;
    }

    public long getDelta() {
        return this.delta;
    }

    public long subtractDelta(long val) {
        this.delta -= val;
        return this.delta;
    }

    public long addDelta(long val) {
        this.delta += val;
        return this.delta;
    }

    public void setDelta(long value) {
        this.delta = value;
    }

    /**
     * Set the object that this node refers to.
     *
     * @param value the object to which this node should refer
     *
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Retrieve the object that this node refers to.
     *
     * @return the object that this node refers to, or <code>null</code> if
     * the object is not set.
     *
     */
    public Object getValue() {
        return this.value;
    }

}








