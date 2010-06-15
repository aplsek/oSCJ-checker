package s3.core.stitcher;

import ovm.core.stitcher.EventServicesFactory;
import ovm.services.io.async.FileDescriptorWrapifier;
import s3.services.io.async.*;

/**
 *
 * @author Filip Pizlo
 */
public class PollingAll_IOServiceConfigurator
    extends BaseIOServiceConfigurator {
    
    protected class AsyncIOServicesFactory
        extends BaseIOServiceConfigurator.AsyncIOServicesFactory {
        
        protected void registerSpecificWrapifiers(FileDescriptorWrapifier wrapifier) {
            ovm.core.stitcher.SystemIOServicesFactory factory=
                (ovm.core.stitcher.SystemIOServicesFactory)
                getServiceFactory(ovm.core.stitcher.SystemIOServicesFactory.name);
            if (factory==null) {
                throw new ovm.util.OVMError("expected SystemIOServicesFactory to already be "+
                                            "configured.");
            }
            
            wrapifier.register(FileDescriptorWrapifier.FD_TYPE_FILE,
                new PollingFileDescriptor.SpecificWrapifier(
		    factory.getIOSignalManagerThatUsesPolling()));
            
            wrapifier.register(FileDescriptorWrapifier.FD_TYPE_UNKNOWN,
                new SignalRWDescriptor.SpecificWrapifier(
                    factory.getIOSignalManagerThatUsesPolling()));
        }

        public String toString() {
            return super.toString() +
                " with Polling for everything";
        }
    }
    
    protected void initFactories() {
        super.initFactories();
        
        if (factories.get(AsyncIOServicesFactory.name) == null)
            factories.put(AsyncIOServicesFactory.name,
                new PollingAll_IOServiceConfigurator.AsyncIOServicesFactory());
        
        if (factories.get(EventServicesFactory.name) == null)
            factories.put(EventServicesFactory.name, new BasicEventServicesFactory());
    }
}

