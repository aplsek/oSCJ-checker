package s3.core.stitcher;

import ovm.core.stitcher.IOServiceConfigurator;
import ovm.core.services.memory.MemoryPolicy;

/**
 * @author Filip Pizlo, David Holmes
 */
public class BaseIOServiceConfigurator extends IOServiceConfigurator {

    protected static class BasicEventServicesFactory 
        extends ovm.core.stitcher.EventServicesFactory {
        public ovm.core.services.events.EventManager getEventManager() {
            return s3.core.services.events.EventManagerImpl.getInstance();
        }
        
        public String toString() {
            return "basic RT event support";
        }
    }


    protected static class SystemIOServicesFactory
        extends ovm.core.stitcher.SystemIOServicesFactory {
        
        public ovm.services.io.signals.IOSignalManager
            getIOSignalManagerThatUsesSIGIO() {
            return s3.services.io.signals.IOSignalManagerViaSIGIO.getInstance();
        }
        
        public ovm.services.io.signals.IOSignalManager
            getIOSignalManagerThatUsesSelect() {
            return null;    // don't have one that uses select
        }
                              
        public ovm.services.io.signals.IOPollingManager
            getIOSignalManagerThatUsesPolling() {
            return s3.services.io.signals.IOSignalManagerViaPolling.getInstance();
        }
                              
        public String toString() {
            return "System-level I/O support with SIGIO and Polling";
        }
    }
    
    
    protected static abstract class AsyncIOServicesFactory
        extends ovm.core.stitcher.AsyncIOServicesFactory {
        
        private ovm.services.io.async.FileDescriptorWrapifier fdWrapifier_ = null;
        
        protected abstract void registerSpecificWrapifiers(
            ovm.services.io.async.FileDescriptorWrapifier wrapifier);
        
        public ovm.services.io.async.FileDescriptorWrapifier getFileDescriptorWrapifier() {
            if (fdWrapifier_ == null) {
		Object save=MemoryPolicy.the().enterServiceInstanceArea();
		try {
		    fdWrapifier_ = new s3.services.io.async.FileDescriptorWrapifierImpl();
		    
		    registerSpecificWrapifiers(fdWrapifier_);
		} finally {
		    MemoryPolicy.the().leave(save);
		}
            }
            
            return fdWrapifier_;
        }
        
        public ovm.services.io.async.StdIOManager getStdIOManager() {
            return s3.services.io.async.StdIOManagerImpl.getInstance();
        }
        public ovm.services.io.async.PipeManager getPipeManager() {
            return s3.services.io.async.PipeManagerImpl.getInstance();
        }
        public ovm.services.io.async.FileManager getFileManager() {
            return s3.services.io.async.FileManagerImpl.getInstance();
        }
        public ovm.services.io.async.SocketManager getSocketManager() {
            return s3.services.io.async.SocketManagerImpl.getInstance();
        }
        public ovm.services.io.async.SelectorManager getSelectorManager() {
            return s3.services.io.async.SelectorManagerImpl.getInstance();
        }
        public ovm.services.io.async.HostLookupManager getHostLookupManager() {
            return s3.services.io.async.ForkingHostLookupManager.getInstance();
        }
        public String toString() {
            return "Async I/O support";
        }
    }
    
    
    protected void initFactories() {
        if (factories.get(SystemIOServicesFactory.name) == null)
            factories.put(SystemIOServicesFactory.name, 
                          new BaseIOServiceConfigurator.SystemIOServicesFactory());
    }


    public void printConfiguration() {
        d("#############################################");
        d("OVM Runtime I/O Service Configuration:");
        String[] services = factories.keys();
        for (int i = 0; i < services.length; i++) {
            d(services[i]);
            d("   " + factories.get(services[i]));
        }
        d("#############################################\n");
    }


}






