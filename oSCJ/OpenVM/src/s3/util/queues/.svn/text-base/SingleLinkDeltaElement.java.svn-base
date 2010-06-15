/*
 * SingleLinkDeltaElement.java
 *
 * Created 6 December 2001, 15:30
 *
 * $Header: /p/sss/cvs/OpenVM/src/s3/util/queues/SingleLinkDeltaElement.java,v 1.6 2007/06/03 01:25:46 baker29 Exp $
 */
package s3.util.queues;


/**
 * A <code>SingleLinkDeltaElement</code> is similar to a 
 * {@link SingleLinkElement} but can be used in a 
 * {@link SingleLinkDeltaQueue delta queue} or
 * {@link SingleLinkTimerQueue timer queue}
 * The two interfaces are different so that a single object can implement
 * both and be in both a delta queue and a normal queue at the same time.
 *
 * @see SingleLinkElement
 * @see SingleLinkDeltaQueue
 *
 * @author David Holmes
 *
 */
public interface SingleLinkDeltaElement {

    /**
     * Set the <code>SingleLinkDeltaElement</code> to which this element
     * is linked.
     *
     * @param next the <code>SingleLinkDeltaElement</code> to which this 
     * element is linked
     *
     */
    void setNextDelta(SingleLinkDeltaElement next);

    /**
     * Retrieve the <code>SingleLinkDeltaElement</code> to which this element
     * is linked.
     *
     * @return the <code>SingleLinkDeltaElement</code> to which this element is
     * linked, or <code>null</code> if there is none.
     *
     */
    SingleLinkDeltaElement getNextDelta();

    /**
     * Returns the delta value associated with this element. 
     * The delta tells you how many events this object is waiting for.
     *
     * @return the delta value associated with this object.
     *
     */
    long getDelta();

    /**
     * Subtracts the specified value from the delta value and returns the
     * new value.
     *
     * @param val the value to subtract.
     *
     * @return the new delta value
     */
    long subtractDelta(long val);

    /**
     * Adds the specified value from the delta value and returns the
     * new value.
     *
     * @param val the value to add.
     *
     * @return the new delta value
     */
    long addDelta(long val);

    /**
     * Sets the delta value to the specified value
     *
     * @param val the new value of the delta
     *
     */
    void setDelta(long val);
}














