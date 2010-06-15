package s3.services.io.async;

import ovm.core.stitcher.AsyncIOServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.IOServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.services.io.async.FileDescriptorWrapifier;
import ovm.services.threads.UserLevelThreadManager;
import ovm.util.OVMError;

/**
 *
 * @author Filip Pizlo
 */
abstract class AsyncIOManagerBase
    extends ovm.services.ServiceInstanceImpl {
    
    protected UserLevelThreadManager tm;
    protected FileDescriptorWrapifier wrapifier;
    
    public void init() {
        tm = (UserLevelThreadManager)
            ((ThreadServicesFactory)ThreadServiceConfigurator.config.
                getServiceFactory(ThreadServicesFactory.name)).getThreadManager();
        if (tm == null) {
            throw new OVMError.Configuration("need a configured thread manager");
        }
        
        wrapifier = ((AsyncIOServicesFactory)IOServiceConfigurator.config.
            getServiceFactory(AsyncIOServicesFactory.name)).
            getFileDescriptorWrapifier();
        if (wrapifier == null) {
            throw new OVMError.Configuration(
                "need a configured file descriptor wrapifier");
        }
        isInited=true;
    }
}

