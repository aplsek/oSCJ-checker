package s3.core.stitcher;


import ovm.core.services.io.BasicIO;
import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.OVMThreadContext;
import ovm.services.java.JavaDispatcher;
import ovm.services.java.JavaMonitor;
import ovm.services.java.JavaOVMThread;
import ovm.services.java.UnsafeJavaMonitor;
import ovm.services.monitors.Monitor;
import ovm.core.services.memory.*;

/**
 * Configuration for a user-domain SCJ compliant Java Virtual Machine
 */
public class SCJJVMConfigurator extends JVMConfigurator {
    /**
     * A thread object to attach to the boot context so that it can be
     * handled by the threading subsystem.
     * This thread provides the necessary support for the Java Virtual Machine
     */
    static class PrimordialRTJavaThread 
        extends s3.services.java.realtime.RealtimeJavaThreadImpl {
        PrimordialRTJavaThread(OVMThreadContext ctx) {
            super(ctx);
        }

        public String toString() {
            return "Primordial RT-Java OVM thread";
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
        extends JVMConfigurator.ThreadServicesFactory {
        public OVMThread getPrimordialThread(OVMThreadContext primordialCtx) {
            if (primordialThread == null) {
                VM_Area prev = MemoryManager.the().
                    setCurrentArea(MemoryManager.the().getImmortalArea());
                try {
                    primordialThread = new PrimordialRTJavaThread(primordialCtx);
                } finally {
                    MemoryManager.the().setCurrentArea(prev);
                }
            }
            return primordialThread;
        }
        
        public String toString() {
            return "Realtime Java threading for a user-domain RTSJ JVM";
        }
        
    }

    protected static class ThreadDispatchServicesFactory
        extends ovm.core.stitcher.ThreadDispatchServicesFactory  {
        
        public OVMDispatcher getThreadDispatcher() {
            return (JavaDispatcher)s3.services.java.realtime.RealtimeJavaDispatcherImpl.getInstance();
        }
        
        public String toString() {
            return "Realtime Java threading for a user-domain RTSJ JVM";
        }
    }

    protected static class MonitorServicesFactory 
        extends ovm.core.stitcher.MonitorServicesFactory {
        public Monitor.Factory getMonitorFactory() {
            return s3.services.java.realtime.PriorityInheritanceJavaMonitorImpl.factory;
        }
        public String toString() {
            return "Realtime Java monitors with priority inheritance  for a user-domain RTSJ JVM";
        }
    }
        
    protected static class JavaServicesFactory extends
                                     JVMConfigurator.JavaServicesFactory {

        public JavaMonitor.Factory getJavaMonitorFactory() {
            return (JavaMonitor.Factory)s3.services.java.realtime.PriorityInheritanceJavaMonitorImpl.factory;
        }

        public UnsafeJavaMonitor.Factory getUnsafeJavaMonitorFactory() {
            return (UnsafeJavaMonitor.Factory)s3.services.java.realtime.UnsafePriorityInheritanceJavaMonitorImpl.factory;
        }

        public JavaDispatcher getJavaDispatcher() {
            return (JavaDispatcher)s3.services.java.realtime.RealtimeJavaDispatcherImpl.getInstance();
        }

        public JavaOVMThread.Factory getJavaOVMThreadFactory() {
            return s3.services.java.realtime.RealtimeJavaThreadImpl.factory;
        }

        public String toString() {
            return "RTSJ compliant Java Virtual Machine for the user-domain";
        }
    }

    protected void initFactories() {
        if (factories.get(MonitorServicesFactory.name) == null) 
            factories.put(MonitorServicesFactory.name, 
                          new SCJJVMConfigurator.MonitorServicesFactory());
        
        if (factories.get(ThreadServicesFactory.name) == null)         
            factories.put(ThreadServicesFactory.name, 
                          new SCJJVMConfigurator.ThreadServicesFactory());
        
        if (factories.get(ThreadDispatchServicesFactory.name) == null)
            factories.put(ThreadDispatchServicesFactory.name,
                          new SCJJVMConfigurator.ThreadDispatchServicesFactory());
        if (factories.get(JavaServicesFactory.name) == null) 
            factories.put(JavaServicesFactory.name, 
                          new SCJJVMConfigurator.JavaServicesFactory());

        super.initFactories();
    }
}