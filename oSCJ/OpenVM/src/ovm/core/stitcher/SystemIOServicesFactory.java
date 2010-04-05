// $Header: /p/sss/cvs/OpenVM/src/ovm/core/stitcher/SystemIOServicesFactory.java,v 1.6 2004/03/08 18:57:26 pizlofj Exp $

package ovm.core.stitcher;

import ovm.services.ServiceInstance;
import ovm.services.io.signals.*;

/**
 * A service factory for IO-related objects that provide
 * low-level system services.
 * <p>
 * This factory will in the future contain such things
 * as AIO management objects and pthread management
 * objects (for getting async syscalls to work by using
 * worker threads).
 *
 * @author Filip Pizlo
 * @author David Holmes
 */
public abstract class SystemIOServicesFactory implements ServiceFactory {
    
    /** Our name */
    public static final String name = "SystemIOServices";
    
    /**
     * Return the IO signal manager that uses SIGIO.
     */
    public abstract IOSignalManager getIOSignalManagerThatUsesSIGIO();
    
    /**
     * Return the IO signal manager that uses select (or something with compatible
     * semantics).
     */
    public abstract IOSignalManager getIOSignalManagerThatUsesSelect();
    
    /**
     * Return the IO signal manager that uses polling.
     */
    public abstract IOPollingManager getIOSignalManagerThatUsesPolling();
    
    public ovm.services.ServiceInstance[] getServiceInstances() {
        return new ServiceInstance[]{getIOSignalManagerThatUsesSIGIO(),
                                     getIOSignalManagerThatUsesSelect(),
                                     getIOSignalManagerThatUsesPolling()};
    }
    
}

