
package ovm.core.stitcher;


import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.OVMThreadContext;
import ovm.core.services.threads.ThreadManager;

/**
 *  A service factory for providing the configuration objects
 *  relating to the core threading subsystem.
 *
 * @author David Holmes
 */
public abstract class ThreadServicesFactory implements ServiceFactory {

    /** The name of this service factory */
    public static final String name = "ThreadServices";
    /**
     * Return the thread manager used in the current configuration.
     * @return the thread manager used in the current configuration.
     *
     */
    public abstract ThreadManager getThreadManager();
    /**
     * Return the thread object that is to represent the primordial
     * thread. This object is bound to the initial execution context
     * created within the OVM.
     * @param ctx the initial execution context
     * @return the primordial thread object
     *
     */
    public abstract OVMThread getPrimordialThread(OVMThreadContext ctx);
    
    public ovm.services.ServiceInstance[] getServiceInstances() {
        return new ovm.services.ServiceInstance[]{getThreadManager()};
    }

}
