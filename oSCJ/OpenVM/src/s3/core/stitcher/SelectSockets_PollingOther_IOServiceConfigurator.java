
package s3.core.stitcher;

import ovm.core.stitcher.EventServicesFactory;
import ovm.services.io.async.FileDescriptorWrapifier;
import s3.services.io.async.PollingFileDescriptor;
import s3.services.io.async.SignalRWDescriptor;
import s3.services.io.async.SignalSocketDescriptor;

/**
 *
 * @author Filip Pizlo
 */
public class SelectSockets_PollingOther_IOServiceConfigurator
    extends BaseIOServiceConfigurator {
    
    protected static class SelectEventServicesFactory 
        extends ovm.core.stitcher.EventServicesFactory {
        public ovm.core.services.events.EventManager getEventManager() {
            return s3.services.events.EventAndIOManagerViaSelect.getInstance();
        }
        
        public String toString() {
            return "select-based event support";
        }
    }
    
    protected static class SystemIOServicesFactoryWithSelect
        extends SystemIOServicesFactory {
        public ovm.services.io.signals.IOSignalManager
            getIOSignalManagerThatUsesSelect() {
            return s3.services.events.EventAndIOManagerViaSelect.getInstance();
        }
                              
        public String toString() {
            return "System-level I/O support with Select and Polling";
        }
    }

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
            
            wrapifier.register(FileDescriptorWrapifier.FD_TYPE_SOCKET,
                new SignalSocketDescriptor.SpecificWrapifier(
                    factory.getIOSignalManagerThatUsesSelect()));
            
            wrapifier.register(FileDescriptorWrapifier.FD_TYPE_FILE,
                new PollingFileDescriptor.SpecificWrapifier(
		    factory.getIOSignalManagerThatUsesPolling()));
            
            wrapifier.register(FileDescriptorWrapifier.FD_TYPE_UNKNOWN,
                new SignalRWDescriptor.SpecificWrapifier(
                    factory.getIOSignalManagerThatUsesPolling()));
        }

        public String toString() {
            return super.toString() +
                " with select for sockets and Polling for everything else";
        }
    }
    
    protected void initFactories() {
        if (factories.get(SystemIOServicesFactory.name) == null)
            factories.put(SystemIOServicesFactory.name, 
                          new SystemIOServicesFactoryWithSelect());

        if (factories.get(AsyncIOServicesFactory.name) == null)
            factories.put(AsyncIOServicesFactory.name,
                new SelectSockets_PollingOther_IOServiceConfigurator.AsyncIOServicesFactory());
        
        if (factories.get(EventServicesFactory.name) == null)
            factories.put(EventServicesFactory.name, new SelectEventServicesFactory());

        super.initFactories();
    }
}

