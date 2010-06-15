/**
 * PriorityCeilingMonitor.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/monitors/PriorityCeilingMonitor.java,v 1.3 2004/02/20 08:48:46 jthomas Exp $
 *
 */
package ovm.services.monitors;

/**
 * Defines a mix-in interface for monitors that can be used with one of the
 * priority inheritance protocols involving a priority ceiling. Examples of
 * such protocols are the Priority Ceiling Protocol and the Priority Ceiling
 * Emulation Protocol.
 * <p>A monitor may restrict the time at which its ceiling may be set and
 * throw an {@link IllegalStateException} if attempted at the wrong time.
 *  Whether or not the ceiling may be set can be queried using
 * the {@link #canSetPriorityCeiling} method.
 * Some implementations may only allow the ceiling to be set
 * at construction time, while others may allow it as long as the monitor
 * is not currently owned (as would be used in a monitor cache), while
 * others might support changing of the ceiling at any time.
 * An implementation must document the exact circumstances under which the
 * ceiling can be set.
 * 
 * @author David Holmes
 *
 */
public interface PriorityCeilingMonitor {

    /**
     * Returns the priority ceiling value of this monitor.
     * @return the priority ceiling value of this monitor.
     *
     */
    public int getPriorityCeiling();

    /**
     * Sets the priority ceiling value of this monitor.
     * A monitor may restrict the time at which its ceiling may be set and
     * throw an {@link IllegalStateException} if this method is invoked at
     * that time. Whether or not the ceiling may be set can be queried using
     * the {@link #canSetPriorityCeiling} method.
     *
     * @param ceiling the priority ceiling value. This value may be 
     * range-checked by an implementation and {@link IllegalArgumentException}
     * thrown if the value if out of range.
     *
     */
    public void setPriorityCeiling(int ceiling);

    /**
     * Queries whether this monitor can have its priority ceiling value
     * changed. 
     * @return <code>true</code> if this monitors ceiling may be changed, and
     * <code>false</code> otherwise.
     *
     */
    public boolean canSetPriorityCeiling();
}

