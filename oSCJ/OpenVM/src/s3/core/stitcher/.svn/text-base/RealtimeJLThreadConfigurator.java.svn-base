package s3.core.stitcher;


import ovm.core.services.threads.OVMDispatcher;

/**
 * Configuration for {@link s3.services.threads.JLThread} with
 * real-time support. This allows use of the executive domain
 * javax.realtime package, but without support for
 * priority-inheritance.
 *
 * @see RealtimeJLThreadPIPConfigurator
 *
 * @author David Holmes
 */
public class RealtimeJLThreadConfigurator extends JLThreadConfigurator {

    protected static class ThreadDispatchServicesFactory
        extends ovm.core.stitcher.ThreadDispatchServicesFactory  {

        public OVMDispatcher getThreadDispatcher() {
            return s3.services.realtime.RealtimeDispatcherImpl.getInstance();
        }
        public String toString() {
            return "Realtime JLThread support: RTSJ-like threads for the kernel(but no priority inheritance";
        }
    }

    protected void initFactories() {
        if (factories.get(ThreadDispatchServicesFactory.name) == null)
            factories.put(ThreadDispatchServicesFactory.name,
                          new RealtimeJLThreadConfigurator.ThreadDispatchServicesFactory());

        super.initFactories();
    }

}






