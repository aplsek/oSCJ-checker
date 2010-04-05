package s3.core.stitcher;

import ovm.core.services.io.BasicIO;
import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.ThreadManager;
import ovm.core.services.threads.OVMThreadContext;
import ovm.services.io.blocking.BlockingManager;

/**
 *
 * A simple threading configuration: non-realtime, non-JVM, with no timer
 * related functionality.
 *
 * @author David Holmes
 */
public class SimpleOVMThreadConfigurator extends BaseThreadServiceConfigurator {

    protected static class BlockingIOServicesFactory 
        extends ovm.core.stitcher.BlockingIOServicesFactory {
        public BlockingManager getBlockingManager() {
            return s3.services.io.blocking.BaselineBlockingManagerImpl.getInstance();
        }
                              
        public String toString() {
            return "Blocking I/O support for plain OVM threads";
        }
    }

    // a special thread type for the primordial thread
    protected static class PrimordialThread 
        extends s3.services.threads.BasicPriorityOVMThreadImpl {
        PrimordialThread(OVMThreadContext ctx) {
            super(ctx);
        }
        protected void doRun() {
            BasicIO.err.print("ERROR: Primordial thread executing in default context - that's not right!\n");
        }
        public String toString() {
            return "Primordial OVM thread";
        }
    }

    protected static class ThreadServicesFactory
        extends ovm.core.stitcher.ThreadServicesFactory {
        // the thread manager class we're using isn't a singleton
        // per se, but we only have one thread manager per configuration
        // at present. It may be that even with multiple thread managers
        // within a VM they will be setup in such a way (per domain or
        // whatever) that the class can be declared a singleton - if so then
        // we can change this
        ThreadManager tm = 
            new s3.services.threads.BasicUserLevelThreadManagerImpl();

        public ThreadManager getThreadManager() {
            return tm;
        }

        OVMThread primordialThread = null; // can't initialise till runtime

        public OVMThread getPrimordialThread(OVMThreadContext primordialCtx) {
            if (primordialThread == null) {
                primordialThread = new PrimordialThread(primordialCtx);
            }
            return primordialThread;
        }

        public String toString() {
            return "Basic thread support: non-RT, non-JVM, no time related functions";
        }

    }

    protected static class ThreadDispatchServicesFactory
        extends ovm.core.stitcher.ThreadDispatchServicesFactory  {

        public OVMDispatcher getThreadDispatcher() {
            return s3.services.threads.DispatcherImpl.getInstance();
        }

        public String toString() {
            return "Basic thread support: non-RT, non-JVM, no timer functions";
        }
    }



    protected void initFactories() {
        if (factories.get(ThreadServicesFactory.name) == null)
            factories.put(ThreadServicesFactory.name, 
                          new SimpleOVMThreadConfigurator.ThreadServicesFactory());

        if (factories.get(ThreadDispatchServicesFactory.name) == null)
            factories.put(ThreadDispatchServicesFactory.name,
                          new SimpleOVMThreadConfigurator.ThreadDispatchServicesFactory());

        if (factories.get(BlockingIOServicesFactory.name) == null)
            factories.put(BlockingIOServicesFactory.name, 
                          new SimpleOVMThreadConfigurator.BlockingIOServicesFactory());

        super.initFactories();
    }
}






