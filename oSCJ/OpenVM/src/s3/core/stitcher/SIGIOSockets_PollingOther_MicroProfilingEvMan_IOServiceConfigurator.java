
package s3.core.stitcher;

import ovm.core.stitcher.EventServicesFactory;

public class SIGIOSockets_PollingOther_MicroProfilingEvMan_IOServiceConfigurator
    extends SIGIOSockets_PollingOther_IOServiceConfigurator {

    protected static class MicroProfilingEventServicesFactory 
        extends ovm.core.stitcher.EventServicesFactory {
        public ovm.core.services.events.EventManager getEventManager() {
            return s3.core.services.events.MicroProfilingEventManagerImpl.getInstance();
        }
        
        public String toString() {
            return "micro-profiling RT event support";
        }
    }

    protected void initFactories() {
	if (factories.get(EventServicesFactory.name) == null)
	    factories.put(EventServicesFactory.name,
			  new MicroProfilingEventServicesFactory());
	
	super.initFactories();
    }
}

