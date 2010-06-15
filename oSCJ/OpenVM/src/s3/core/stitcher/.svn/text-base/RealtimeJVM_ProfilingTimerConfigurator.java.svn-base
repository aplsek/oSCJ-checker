package s3.core.stitcher;

import ovm.core.services.timer.*;
import s3.core.services.timer.*;

public class RealtimeJVM_ProfilingTimerConfigurator
    extends RealtimeJVMConfigurator {
    
    protected static class ProfilingTimerServicesFactory
        extends ovm.core.stitcher.TimerServicesFactory {
        public TimerManager getTimerManager() {
            return ProfilingTimerManagerImpl.getInstance();
        }
        public String toString() {
            return "Profiling timer manager implementation";
        }
    }
    
    protected void initFactories() {
	if (factories.get(TimerServicesFactory.name)==null)
	    factories.put(TimerServicesFactory.name,
			  new ProfilingTimerServicesFactory());
	
	super.initFactories();
    }
}

