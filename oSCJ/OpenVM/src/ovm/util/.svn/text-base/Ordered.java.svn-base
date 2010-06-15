/**
 * Ordered.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/util/Ordered.java,v 1.6 2004/02/20 08:48:56 jthomas Exp $
 *
 */
package ovm.util;


/**
 * Defines the notion of an object maintaning something in some order.
 * What the actual order is depends on the 
 * {@link java.util.Comparator comparator} associated with the object.
 *
 */
public interface Ordered {

    /**
     * Sets the comparator used by this ordered object.
     * <p>An implementation may restrict the times when this method
     * may be called. Calling this method when it is not permitted may
     * result in an <code>IllegalStateException</code> being thrown.
     * Such restrictions should be documented by the implementing class.
     *
     * @param comp the <code>Comparator</code> to be used
     *
     * @throws IllegalArgumentException if <code>comp</code> is <code>null</code>
     * @throws IllegalStateException if the comparator is already set and this
     * implementation does not support further changes.
     *
     */
    void setComparator(java.util.Comparator comp);

    /**
     * Returns the comparator used by this ordered object
     * @return the comparator used by this ordered object, or 
     * <code>null</code> if the comparator has not been set.
     *
     */
    java.util.Comparator getComparator();

    /**
     * Informs the ordered object that an object the order is dependent upon
     * has changed. The ordered object can then re-assess its order and
     * re-order if necessary. For example, an object maintaining FIFO order
     * can't be affected by a change in any of the objects and so need do
     * nothing. On the other hand a priority order may need the changed item
     * to be removed and re-inserted.
     *
     * @param obj the obj that has changed
     *
     */
    void changeNotification(Object obj);
}

