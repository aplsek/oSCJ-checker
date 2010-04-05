package s3.core.stitcher;

import ovm.core.services.io.BasicIO;
import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.ThreadManager;
import ovm.core.services.threads.OVMThreadContext;
import ovm.core.services.timer.TimerManager;
import ovm.services.io.blocking.BlockingManager;
import ovm.services.java.JavaDispatcher;
import ovm.services.java.JavaMonitor;
import ovm.services.java.JavaOVMThread;
import ovm.services.java.JavaUserLevelThreadManager;
import ovm.services.java.UnsafeJavaMonitor;
import ovm.services.monitors.Monitor;

/**
 * Configuration for a user-domain Java Virtual Machine
 *
 * @author David Holmes
 */
public class JVMConfigurator extends BaseThreadServiceConfigurator {
    
    protected static class MonitorServicesFactory 
        extends ovm.core.stitcher.MonitorServicesFactory {
        public Monitor.Factory getMonitorFactory() {
            return s3.services.java.ulv1.JavaMonitorImpl.factory;
        }
        public String toString() {
            return "Java monitors for a user-domain JVM";
        }
    }
    
    protected static class BlockingIOServicesFactory 
        extends ovm.core.stitcher.BlockingIOServicesFactory {
        public BlockingManager getBlockingManager() {
            return s3.services.java.ulv1.JavaBlockingManagerImpl.getInstance();
        }
                              
        public String toString() {
            return "Blocking I/O support for Java threads";
        }
    }

    /**
     * A thread object to attach to the boot context so that it can be
     * handled by the threading subsystem.
     * This thread provides the necessary support for the Java Virtual Machine
     */
    static class PrimordialJavaThread 
        extends s3.services.java.ulv1.JavaOVMThreadImpl {
        PrimordialJavaThread(OVMThreadContext ctx) {
            super(ctx);
        }
        public String toString() {
            return "Primordial Java OVM thread";
        }
        protected void doRun() {
            BasicIO.err.print("ERROR: Primordial thread executing in default context - that's not right!\n");
        }
        /**
         * Override to only set READY state. We can't set priority until a
         * Java thread is bound to us.
         */
        public void prepareForStart(JavaDispatcher _) {
            setState(READY);
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
        static ThreadManager tm = 
        new s3.services.java.ulv1.JavaThreadManagerImpl();
        
        public ThreadManager getThreadManager() {
            return tm;
        }
        
        OVMThread primordialThread = null; // can't initialise till runtime
        
        public OVMThread getPrimordialThread(OVMThreadContext primordialCtx) {
            if (primordialThread == null) {
                primordialThread = new PrimordialJavaThread(primordialCtx);
            }
            return primordialThread;
        }
        
        public String toString() {
            return "Java threading for a user-domain JVM";
        }
    }
    
    protected static class ThreadDispatchServicesFactory
        extends ovm.core.stitcher.ThreadDispatchServicesFactory  {
        
        public OVMDispatcher getThreadDispatcher() {
            return s3.services.java.ulv1.JavaDispatcherImpl.getInstance();
        }
        
        public String toString() {
            return "Java threading for a user-domain JVM";
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
    
    protected static class JavaServicesFactory 
        extends ovm.core.stitcher.JavaServicesFactory {
        
        public JavaDispatcher getJavaDispatcher() {
            return (JavaDispatcher)s3.services.java.ulv1.JavaDispatcherImpl.getInstance();
        }

        public JavaUserLevelThreadManager getJavaThreadManager() {
            return (JavaUserLevelThreadManager)ThreadServicesFactory.tm;
        }

        public JavaMonitor.Factory getJavaMonitorFactory() {
            return s3.services.java.ulv1.JavaMonitorImpl.factory;
        }

        public UnsafeJavaMonitor.Factory getUnsafeJavaMonitorFactory() {
            return s3.services.java.ulv1.UnsafeJavaMonitorImpl.factory;
        }

        public JavaOVMThread.Factory getJavaOVMThreadFactory() {
            return s3.services.java.ulv1.JavaOVMThreadImpl.factory;
        }

        public String toString() {
            return "Java Virtual Machine for the user-domain";
        }
    }

    protected void initFactories() {
        if (factories.get(MonitorServicesFactory.name) == null) 
            factories.put(MonitorServicesFactory.name, 
                          new JVMConfigurator.MonitorServicesFactory());
        
        if (factories.get(ThreadServicesFactory.name) == null) 
            factories.put(ThreadServicesFactory.name, 
                          new JVMConfigurator.ThreadServicesFactory());

        if (factories.get(ThreadDispatchServicesFactory.name) == null)
            factories.put(ThreadDispatchServicesFactory.name,
                          new JVMConfigurator.ThreadDispatchServicesFactory());

        if (factories.get(TimerServicesFactory.name) == null) 
            factories.put(TimerServicesFactory.name, 
                          new JVMConfigurator.TimerServicesFactory());

        if (factories.get(BlockingIOServicesFactory.name) == null) 
            factories.put(BlockingIOServicesFactory.name, 
                          new JVMConfigurator.BlockingIOServicesFactory());
            
        if (factories.get(JavaServicesFactory.name) == null) 
            factories.put(JavaServicesFactory.name, 
                          new JVMConfigurator.JavaServicesFactory());

        super.initFactories();
    }

}






