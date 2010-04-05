package s3.core.stitcher;

import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.services.monitors.Monitor;
/**
 * A starting point for an OVM runtime configuration. This class supports
 * the event-manager and basic monitors.
 * Subclasses must add specific threading support.
 * <p>Subclasses will define the appropriate factory instances, which in turn
 * will either return singleton service-instances, or a locally held reference
 * to the configured service instance.
 *
 * @author David Holmes, Filip Pizlo
 */
public abstract class BaseThreadServiceConfigurator extends ThreadServiceConfigurator {

    protected static class SignalServicesFactory 
        extends ovm.core.stitcher.SignalServicesFactory {
        public ovm.services.events.SignalMonitor getSignalMonitor() {
            return s3.services.events.SignalMonitorImpl.getInstance();
        }
        public String toString() {
            return "core signal support";
        }
    }

    protected static class InterruptServicesFactory extends ovm.core.stitcher.InterruptServicesFactory {
      public ovm.services.events.InterruptMonitor getInterruptMonitor() {
        return s3.services.events.InterruptMonitorImpl.getInstance();
      }
      
      public String toString() {
        return "core hardware interrupt support";
      }
    }
        

    protected static class MonitorServicesFactory 
        extends ovm.core.stitcher.MonitorServicesFactory {
        public Monitor.Factory getMonitorFactory() {
            return s3.services.monitors.BasicMonitorImpl.factory;
        }
        public String toString() {
            return "executive domain support for monitor entry and exit only";
        }
    }

    
    protected void initFactories() {
        // the event manager can always be configured in terms of
        // functionality
        if (factories.get(SignalServicesFactory.name) == null)
            factories.put(SignalServicesFactory.name, 
                          new BaseThreadServiceConfigurator.SignalServicesFactory());


        // basic monitor support is okay for most configs too
        if (factories.get(MonitorServicesFactory.name) == null)
            factories.put(MonitorServicesFactory.name, 
                          new BaseThreadServiceConfigurator.MonitorServicesFactory());
     
        if (InvisibleStitcher.getString("interrupts").equals("yes")) {                     
          if (factories.get(InterruptServicesFactory.name) == null)
              factories.put(InterruptServicesFactory.name, 
                            new BaseThreadServiceConfigurator.InterruptServicesFactory());                          
        }
    }


    protected BaseThreadServiceConfigurator() {}

    public void printConfiguration() {
        d("#############################################");
        d("OVM Runtime THREAD Service Configuration:");
        String[] services = factories.keys();
        for (int i = 0; i < services.length; i++) {
            d(services[i]);
            d("   " + factories.get(services[i]));
        }
        d("#############################################\n");
    }

}






