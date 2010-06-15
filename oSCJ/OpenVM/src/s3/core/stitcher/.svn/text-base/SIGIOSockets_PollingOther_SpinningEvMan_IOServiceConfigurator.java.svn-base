
package s3.core.stitcher;

import ovm.core.stitcher.EventServicesFactory;

public class SIGIOSockets_PollingOther_SpinningEvMan_IOServiceConfigurator
    extends SIGIOSockets_PollingOther_IOServiceConfigurator {

    protected static class SpinningEventServicesFactory 
        extends ovm.core.stitcher.EventServicesFactory {
        public ovm.core.services.events.EventManager getEventManager() {
            return s3.core.services.events.SpinningEventManagerImpl.getInstance();
        }
        
        public String toString() {
            return "spinning RT event support";
        }
    }

    protected void initFactories() {
	if (factories.get(EventServicesFactory.name) == null)
	    factories.put(EventServicesFactory.name,
			  new SpinningEventServicesFactory());
	
	super.initFactories();
    }
}

