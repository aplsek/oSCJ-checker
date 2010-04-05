package s3.core.stitcher;

import ovm.core.services.io.BasicIO;
import ovm.core.services.timer.TimerManager;

public class ATime_RealtimeJVMConfigurator extends RealtimeJVMConfigurator {
    
    protected static class TimerServicesFactory
        extends ovm.core.stitcher.TimerServicesFactory {
        public TimerManager getTimerManager() {
            return s3.core.services.timer.AbsoluteTimerManagerImpl.getInstance();
        }
        public String toString() {
            return "Absolute time timer manager implementation";
        }
    }
    
    protected void initFactories() {
	if (factories.get(TimerServicesFactory.name)==null) 
	    factories.put(TimerServicesFactory.name,
			  new ATime_RealtimeJVMConfigurator.TimerServicesFactory());
	
	super.initFactories();
    }
    
}
