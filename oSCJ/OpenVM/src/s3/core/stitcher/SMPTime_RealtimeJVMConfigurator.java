package s3.core.stitcher;

import ovm.core.services.io.BasicIO;
import ovm.core.services.timer.TimerManager;

public class SMPTime_RealtimeJVMConfigurator extends RealtimeJVMConfigurator {
    
    protected static class TimerServicesFactory
        extends ovm.core.stitcher.TimerServicesFactory {
        public TimerManager getTimerManager() {
            return s3.core.services.timer.SMPTimerManagerImpl.getInstance();
        }
        public String toString() {
            return "SMP timer manager implementation";
        }
    }
    
    protected void initFactories() {
	if (factories.get(TimerServicesFactory.name)==null) 
	    factories.put(TimerServicesFactory.name,
			  new SMPTime_RealtimeJVMConfigurator.TimerServicesFactory());
	
	super.initFactories();
    }
    
}
