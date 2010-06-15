
package ovm.core.stitcher;

import ovm.services.java.JavaDispatcher;
import ovm.services.java.JavaMonitor;
import ovm.services.java.UnsafeJavaMonitor;
import ovm.services.java.JavaOVMThread;
import ovm.services.java.JavaUserLevelThreadManager;
/**
 *  A service factory for providing the configuration objects
 *  relating to the Java Virtual Machine.
 *
 * @author David Holmes
 */

public abstract class JavaServicesFactory implements ServiceFactory {

    /** The name of this service factory */
    public static final String name = "JavaServices";

    /**
     * Return the Java Dispatcher used in the current configuration
     * @return the Java Dispatcher used in the current configuration
     *
     */
    public abstract JavaDispatcher getJavaDispatcher();

    /**
     * Return the Java thread manager used in the current configuration
     * @return the Java thread manager used in the current configuration
     *
     */
    public abstract JavaUserLevelThreadManager getJavaThreadManager();


    /** 
     * Return the monitor factory used in the current configuration.
     * @return the monitor factory used in the current configuration.
     *
     */
    public abstract JavaMonitor.Factory getJavaMonitorFactory();
    
    /**
     * Return the unsafe monitor factory used in the current configuration.
     * @return the unsafe monitor factory used in the current configuration.
     */
    public abstract UnsafeJavaMonitor.Factory getUnsafeJavaMonitorFactory();

    /**
     * Return the Java OVM thread factory used in the current configuration.
     * @return the Java OVM thread factory used in the current configuration
     *
     */
    public abstract JavaOVMThread.Factory getJavaOVMThreadFactory();
    
    public ovm.services.ServiceInstance[] getServiceInstances() {
        return new ovm.services.ServiceInstance[]{getJavaDispatcher(),
                                                  getJavaThreadManager()};
    }

}










