package ovm.core.stitcher;

import ovm.services.events.InterruptMonitor;

public abstract class InterruptServicesFactory implements ServiceFactory {

    /** The name of this service factory */
    public static final String name = "InterruptServices";

    /**
     * Return the interrupt monitor used in the current configuration.
     * @return the interrupt monitor used in the current configuration.
     *
     */
    public abstract InterruptMonitor  getInterruptMonitor();
    
    public ovm.services.ServiceInstance[] getServiceInstances() {
        return new ovm.services.ServiceInstance[]{
            getInterruptMonitor(),
        };
    }

}
