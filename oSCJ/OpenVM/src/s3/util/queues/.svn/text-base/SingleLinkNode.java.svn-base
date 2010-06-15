/*
 * SingleLinkNode.java
 *
 * Created 6 December 2001, 13:50
 *
 * $Header: /p/sss/cvs/OpenVM/src/s3/util/queues/SingleLinkNode.java,v 1.9 2006/04/20 15:48:42 baker29 Exp $
 */
package s3.util.queues;

import ovm.core.OVMBase;
/**
 * A wrapper object that can form part of a singly-linked list.
 *
 * @see SingleLinkElement
 *
 * @author David Holmes
 *
 */
public class SingleLinkNode implements SingleLinkElement{

    private SingleLinkElement next;
    private Object value;

    /**
     * Create a node that is unlinked and refers to no object.
     *
     */
    public SingleLinkNode() {
        this(null, null);
    }

    /**
     * Create a node that is linked to the given element, but refers to
     * no object. 
     * That is, after construction 
     * <code>this.getNext() == next && this.getValue() == null</code>.
     *
     * @param next the element to which this node should be linked
     *
     */
    public SingleLinkNode(SingleLinkElement next) {
        this(next, null);
    }

    /**
     * Create an unlinked node that refers to the given object. 
     * That is, after construction 
     * <code>this.getNext() == null && this.getValue() == value</code>.
     *
     * @param value the object to which this node should refer
     *
     */
    public SingleLinkNode(Object value) {
        this(null, value);
    }

    /**
     * Create a node that is linked to the given element and refers to the
     * given object. That is, after construction 
     * <code>this.getNext() == next && this.getValue() == value</code>.
     *
     * @param next the element to which this node should be linked
     * @param value the object to which this node should refer
     *
     */
    public SingleLinkNode(SingleLinkElement next, Object value) {
        assert next != this:  "next == this";
        this.next = next;
        this.value = value;
    }


    public void setNext(SingleLinkElement next) {
        assert next != this: "next == this";
        this.next = next;
    }

    public SingleLinkElement getNext() {
        return this.next;
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





