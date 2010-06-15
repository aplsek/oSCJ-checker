package s3.core.stitcher;


import ovm.core.services.io.BasicIO;
import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.ThreadManager;
import ovm.core.services.threads.OVMThreadContext;
import ovm.core.services.timer.TimerManager;

/**
 * Configuration for use of {@link JLThread} - a java-like thread defined for
 * the executive-domain. It is non real-time and supports timer related
 * functionality (ie. sleep).
 *
 * @author David Holmes
 */
public class JLThreadConfigurator extends BaseThreadServiceConfigurator {

    protected static class BlockingIOServicesFactory
        extends ovm.core.stitcher.BlockingIOServicesFactory {
        public ovm.services.io.blocking.BlockingManager getBlockingManager() {
            return s3.services.io.blocking.JLThreadBlockingManagerImpl.getInstance();
        }
        public String toString() {
            return "Blocking I/O support for JLThread";
        }
    }
    
    /**
     * A JLThread object to attach to the boot context so that it can be
     * handled by the threading subsystem.
     * This is a priority thread which also supports sleeping and a whole
     * lot more.
     */
    static class PrimordialJLThread extends s3.services.threads.JLThread {
        PrimordialJLThread(OVMThreadContext ctx) {
            super(ctx, "Primordial OVM Thread");
            // these would be set by JLThread.start, but we don't invoke that
            lifecycleState = STARTED_THREAD; 
            executionState = READY;             
        }
        protected void executeRun() {
            BasicIO.err.print("ERROR: Primordial thread executing in default context - that's not right!\n");
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
           new s3.services.threads.TimedSuspensionUserLevelThreadManagerImpl();

        public ThreadManager getThreadManager() {
            return tm;
        }

        OVMThread primordialThread = null; // can't initialise till runtime

        public OVMThread getPrimordialThread(OVMThreadContext primordialCtx) {
            if (primordialThread == null) {
                primordialThread = new PrimordialJLThread(primordialCtx);
            }
            return primordialThread;
        }

        public String toString() {
            return "Timed-suspension user-level thread manager with JLThread support";
        }

    }

    protected static class ThreadDispatchServicesFactory
        extends ovm.core.stitcher.ThreadDispatchServicesFactory  {

        public OVMDispatcher getThreadDispatcher() {
            return s3.services.threads.PriorityDispatcherImpl.getInstance();
        }

        public String toString() {
            return "JLThread support: java-like priority threads for the kernel";
        }
    }

    protected static class TimerServicesFactory
        extends ovm.core.stitcher.TimerServicesFactory {
        public TimerManager getTimerManager() {
            return s3.core.services.timer.TimerManagerImpl.getInstance();
        }
        public String toString() {
            return "Core timer manager implementation";
        }
    }

    protected void initFactories() {
        if (factories.get(ThreadServicesFactory.name) == null) 
            factories.put(ThreadServicesFactory.name, 
                          new JLThreadConfigurator.ThreadServicesFactory());
        
        if (factories.get(ThreadDispatchServicesFactory.name) == null)
            factories.put(ThreadDispatchServicesFactory.name,
                          new JLThreadConfigurator.ThreadDispatchServicesFactory());
        if (factories.get(TimerServicesFactory.name) == null) 
            factories.put(TimerServicesFactory.name, 
                          new JLThreadConfigurator.TimerServicesFactory());
        
        if (factories.get(BlockingIOServicesFactory.name) == null) 
            factories.put(BlockingIOServicesFactory.name, 
                          new JLThreadConfigurator.BlockingIOServicesFactory());
        
        super.initFactories();
    }


}






