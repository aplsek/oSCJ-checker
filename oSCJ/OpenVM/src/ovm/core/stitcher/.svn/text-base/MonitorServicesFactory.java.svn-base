/**
 * MonitorServicesFactory.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/core/stitcher/MonitorServicesFactory.java,v 1.8 2004/02/20 08:48:13 jthomas Exp $
 *
 */
package ovm.core.stitcher;

import ovm.services.monitors.Monitor;
/**
 * A service factory for providing the configuration objects
 * relating to the use of Monitors for simple thread synchronization.
 * <p>A more sophisticated configuration, like the Java Virtual Machine,
 * will provide its own services for this.
 *
 * @see JavaServicesFactory#getJavaMonitorFactory
 *
 * @author David Holmes
 */

public abstract class MonitorServicesFactory implements ServiceFactory {

    /** The name of this service factory */
    public static final String name = "MonitorServices";

    /** 
     * Return the monitor factory used in the current configuration.
     * @return the monitor factory used in the current configuration.
     *
     */
    public abstract Monitor.Factory getMonitorFactory();
    
    public ovm.services.ServiceInstance[] getServiceInstances() {
        return new ovm.services.ServiceInstance[0];
    }
    
}






