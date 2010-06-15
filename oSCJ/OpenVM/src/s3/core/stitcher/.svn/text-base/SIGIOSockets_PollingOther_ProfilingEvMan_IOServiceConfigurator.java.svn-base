
package s3.core.stitcher;

import ovm.core.stitcher.EventServicesFactory;

public class SIGIOSockets_PollingOther_ProfilingEvMan_IOServiceConfigurator
    extends SIGIOSockets_PollingOther_IOServiceConfigurator {

    protected static class ProfilingEventServicesFactory 
        extends ovm.core.stitcher.EventServicesFactory {
        public ovm.core.services.events.EventManager getEventManager() {
            return s3.core.services.events.ProfilingEventManagerImpl.getInstance();
        }
        
        public String toString() {
            return "profiling RT event support";
        }
    }

    protected void initFactories() {
	if (factories.get(EventServicesFactory.name) == null)
	    factories.put(EventServicesFactory.name,
			  new ProfilingEventServicesFactory());
	
	super.initFactories();
    }
}

