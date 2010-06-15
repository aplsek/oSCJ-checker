/**
 * $Header: /p/sss/cvs/OpenVM/src/ovm/core/stitcher/ThreadDispatchServicesFactory.java,v 1.4 2004/02/20 08:48:13 jthomas Exp $
 *
 */
package ovm.core.stitcher;

import ovm.core.services.threads.OVMDispatcher;

/**
 *  A service factory for providing the configuration objects
 *  relating to optional thread dispatching.
 * <p>The {@link OVMDispatcher} provides a higher-level
 * threading API above that of a simple 
 * {@link ovm.core.services.threads.ThreadManager thread manager}.
 * A more sophisticated configuration, such as a full Java Virtual Machine,
 * will provide its own thread dispatching facilities.
 *
 * @see JavaServicesFactory#getJavaDispatcher
 *
 * @author David Holmes
 */
public abstract class ThreadDispatchServicesFactory implements ServiceFactory {

    /** The name of this service factory */
    public static final String name = "ThreadDispatchServices";

    /**
     * Return the OVM thread dispatcher used in the current configuration.
     * @return the OVM thread dispatcher used in the current configuration.
     *
     */
    public abstract OVMDispatcher getThreadDispatcher();
    
    public ovm.services.ServiceInstance[] getServiceInstances() {
        return new ovm.services.ServiceInstance[]{getThreadDispatcher()};
    }

}
