// $Header: /p/sss/cvs/OpenVM/src/ovm/core/stitcher/BlockingIOServicesFactory.java,v 1.3 2004/02/20 08:48:13 jthomas Exp $

package ovm.core.stitcher;

/**
 *
 * @author Filip Pizlo
 */
public abstract class BlockingIOServicesFactory implements ServiceFactory {
    
    public static final String name = "BlockingIOServices";
    
    public abstract ovm.services.io.blocking.BlockingManager getBlockingManager();
    
    public ovm.services.ServiceInstance[] getServiceInstances() {
        return new ovm.services.ServiceInstance[]{getBlockingManager()};
    }
    
}

