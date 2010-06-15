/**
 * $Header: /p/sss/cvs/OpenVM/src/ovm/core/stitcher/EventServicesFactory.java,v 1.7 2004/02/17 19:18:09 jv Exp $
 */
package ovm.core.stitcher;

import ovm.core.services.events.EventManager;

/**
 *  A service factory for providing the configuration objects
 *  relating to the I/O event management subsytem.
 *
 * @author David Holmes
 */
public abstract class EventServicesFactory implements ServiceFactory {

    /** The name of this service factory */
    public static final String name = "EventServices";

    /**
     * Return the event manager used in the current configuration.
     * @return the event manager used in the current configuration.
     *
     */
    public abstract EventManager getEventManager();

    public ovm.services.ServiceInstance[] getServiceInstances() {
        return new ovm.services.ServiceInstance[]{
            getEventManager(),
        };
    }

}
