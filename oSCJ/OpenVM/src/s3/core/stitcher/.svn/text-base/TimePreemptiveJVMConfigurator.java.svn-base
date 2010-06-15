package s3.core.stitcher;

import ovm.core.services.threads.OVMDispatcher;
import ovm.services.java.JavaDispatcher;

/**
 * Configuration for a user-domain Java Virtual Machine with a time-preemptive
 * dispatcher.
 *
 * @author David Holmes
 */
public class TimePreemptiveJVMConfigurator extends JVMConfigurator {
    
    // extend to override getThreadDispatcher
    protected static class ThreadDispatchServicesFactory
        extends JVMConfigurator.ThreadDispatchServicesFactory  {
        
        public OVMDispatcher getThreadDispatcher() {
            return s3.services.java.ulv1.TimePreemptiveJavaDispatcher.getInstance();
        }
        
        public String toString() {
            return "Java threading with time-preemption for a user-domain JVM";
        }
    }
    

    // extend to override getJavaDispacther
    protected static class JavaServicesFactory 
        extends JVMConfigurator.JavaServicesFactory {
        
        public JavaDispatcher getJavaDispatcher() {
            return (JavaDispatcher)s3.services.java.ulv1.TimePreemptiveJavaDispatcher.getInstance();
        }

        public String toString() {
            return "Java Virtual Machine for the user-domain - with time preemption";
        }
    }

    protected void initFactories() {
        if (factories.get(ThreadDispatchServicesFactory.name) == null)
            factories.put(ThreadDispatchServicesFactory.name,
                          new TimePreemptiveJVMConfigurator.ThreadDispatchServicesFactory());

        if (factories.get(JavaServicesFactory.name) == null) 
            factories.put(JavaServicesFactory.name, 
                          new TimePreemptiveJVMConfigurator.JavaServicesFactory());

        super.initFactories();
    }

}






