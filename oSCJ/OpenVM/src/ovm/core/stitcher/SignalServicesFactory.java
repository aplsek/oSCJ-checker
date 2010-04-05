package ovm.core.stitcher;

import ovm.services.events.SignalMonitor;

/**
 *  A service factory for providing the configuration objects
 *  relating to the signal management sybsystem.
 *
 * @author Filip Pizlo, David Holmes
 */
public abstract class SignalServicesFactory implements ServiceFactory {

    /** The name of this service factory */
    public static final String name = "SignalServices";

    /**
     * Return the signal monitor used in the current configuration.
     * @return the signal monitor used in the current configuration.
     *
     */
    public abstract SignalMonitor  getSignalMonitor();
    
    public ovm.services.ServiceInstance[] getServiceInstances() {
        return new ovm.services.ServiceInstance[]{
            getSignalMonitor(),
        };
    }

}
