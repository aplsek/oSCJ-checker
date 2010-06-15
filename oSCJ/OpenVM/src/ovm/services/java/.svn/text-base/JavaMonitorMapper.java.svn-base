/**
 * JavaMonitorMapper.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/java/JavaMonitorMapper.java,v 1.5 2004/02/20 08:48:41 jthomas Exp $
 *
 */
package ovm.services.java;


/**
 * Provides a mapping between Java objects and {@link JavaMonitor Java 
 * monitors}.
 *
 * @see JavaMonitor
 *
 * @author David Holmes
 *
 */
public interface JavaMonitorMapper {

    /**
     * Returns the monitor with which the specified Java object is associated.
     * Some possible implementation strategies are to return the object itself
     * if each object holds its own monitor, to return a helper object that
     * provides monitor functionality, to lookup a monitor cache etc.
     *
     * @param obj the object whose monitor is to be located.
     * @return the monitor for the specified object. This value must never
     * be <code>null</code>.
     *
     */
    JavaMonitor getMonitorFor(Object obj);

}
