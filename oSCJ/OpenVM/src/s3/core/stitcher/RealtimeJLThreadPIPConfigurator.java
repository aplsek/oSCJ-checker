package s3.core.stitcher;

import ovm.core.services.threads.OVMDispatcher;
import ovm.services.monitors.Monitor;

/**
 * Configuration for {@link s3.services.threads.JLThread} with
 * real-time support. This allows use of the executive domain
 * javax.realtime package, including support for priority-inheritance.
 *
 * @see RealtimeJLThreadPIPConfigurator
 *
 * @author David Holmes
 */
public class RealtimeJLThreadPIPConfigurator 
    extends RealtimeJLThreadConfigurator {

    protected static class ThreadDispatchServicesFactory
        extends ovm.core.stitcher.ThreadDispatchServicesFactory  {

        public OVMDispatcher getThreadDispatcher() {
            return s3.services.realtime.PriorityInheritanceDispatcherImpl.getInstance();
        }
        public String toString() {
            return "Realtime JLThread support: RTSJ-like threads with priority inheritance";
        }
    }


    protected static class MonitorServicesFactory 
        extends ovm.core.stitcher.MonitorServicesFactory {
        public Monitor.Factory getMonitorFactory() {
            return s3.services.realtime.PriorityInheritanceMonitorImpl.factory;
        }
        public String toString() {
            return "Real-time executive domain support for monitor entry and exit only - with priority inheritance";
        }
    }

    protected void initFactories() {
        if (factories.get(MonitorServicesFactory.name) == null)
            factories.put(MonitorServicesFactory.name, 
                          new RealtimeJLThreadPIPConfigurator.MonitorServicesFactory());
        
        if (factories.get(ThreadDispatchServicesFactory.name) == null)
            factories.put(ThreadDispatchServicesFactory.name,
                          new RealtimeJLThreadPIPConfigurator.ThreadDispatchServicesFactory());

        super.initFactories();
    }
}






