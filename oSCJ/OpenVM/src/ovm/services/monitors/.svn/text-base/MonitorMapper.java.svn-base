/**
 * MonitorMapper.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/monitors/MonitorMapper.java,v 1.11 2004/06/06 19:52:05 baker29 Exp $
 *
 */
package ovm.services.monitors;

import ovm.core.domain.Oop;
import ovm.core.domain.ObjectModel.PragmaModelOp;

/**
 * Provides a mapping between objects and {@link Monitor monitors}.
 *
 * @see Monitor
 *
 * @author David Holmes
 */
public interface MonitorMapper extends Oop {
    String ID = "Lovm/services/monitors/MonitorMapper;";
    
    /**
     * Returns the monitor with which this Oop is associated.
     * Some possible implementation strategies are to return the object itself
     * if each object holds its own monitor, to return a helper object that
     * provides monitor functionality, to lookup a monitor cache etc.
     * This method may return null, in which case a monitor must be
     * allocated manually, and associated with this object through 
     * {@link #setMonitor(Monitor) setMonitor}). 
     *
     * @return the monitor for this Oop. This value may
     * be <code>null</code>.
     */
    Monitor getMonitor() throws PragmaModelOp;

    /**
     * Sets the monitor associated with this Oop to the given monitor.
     * <p>This allows monitors to be shared.
     * @param monitor the monitor to set
     */
    void setMonitor(Monitor monitor) throws PragmaModelOp;

    /**
     * Releases the resources associated with the monitor for this Oop.
     * This should be called by the garbage collector as part of
     * reclaiming an object.
     * <p>
     * FIXME: This method is not called.  We do need an API for
     * callbacks as objects are freed, but this API is just really,
     * really bad.  And, so far we don't need any callbacks for the
     * object associated with a monitor being freed.
     */
    void releaseMonitor() throws PragmaModelOp;
}


