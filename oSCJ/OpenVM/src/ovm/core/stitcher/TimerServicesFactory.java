/**
 * TimerServicesFactory.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/core/stitcher/TimerServicesFactory.java,v 1.3 2004/02/20 08:48:13 jthomas Exp $
 *
 */
package ovm.core.stitcher;

import ovm.core.services.timer.TimerManager;
/**
 *  A service factory for providing the configuration objects
 *  relating to the timer subsystem.
 *
 * @author David Holmes
 */
public abstract class TimerServicesFactory implements ServiceFactory {

    /** The name of this service factory */
    public static final String name = "TimerServices";

    /**
     * Return the timer manager used in the current configuration.
     * @return the timer manager used in the current configuration.
     *
     */
    public abstract TimerManager getTimerManager();
    
    public ovm.services.ServiceInstance[] getServiceInstances() {
        return new ovm.services.ServiceInstance[]{getTimerManager()};
    }
}
