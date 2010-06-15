/*
 * SingleLinkElement.java
 *
 * Created 6 December 2001, 13:50
 *
 * $Header: /p/sss/cvs/OpenVM/src/s3/util/queues/SingleLinkElement.java,v 1.3 2004/02/20 08:53:16 jthomas Exp $
 */
package s3.util.queues;


/**
 * A <code>SingleLinkElement</code> is an object that can form part of a
 * singly-linked list. It is an optimisation for creating lists of objects
 * without having to create a list-aware wrapper objects for them.
 * A <code>SingleLinkElement</code> can only exist in one list at a time.
 *
 * @see SingleLinkNode
 *
 * @author David Holmes
 *
 */
public interface SingleLinkElement {

    /**
     * Set the <code>SingleLinkElement</code> to which this element is linked.
     *
     * @param next the <code>SingleLinkElement</code> to which this element
     * is linked
     *
     */
    void setNext(SingleLinkElement next);

    /**
     * Retrieve the <code>SingleLinkElement</code> to which this element
     * is linked.
     *
     * @return the <code>SingleLinkElement</code> to which this element is
     * linked, or <code>null</code> if there is none.
     *
     */
    SingleLinkElement getNext();

}






