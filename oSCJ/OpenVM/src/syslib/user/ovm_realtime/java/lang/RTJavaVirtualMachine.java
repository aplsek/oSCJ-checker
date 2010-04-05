package java.lang;

import javax.realtime.ImmortalMemory;
import javax.realtime.MemoryAccessError;
import javax.realtime.NoHeapRealtimeThread;
/**
 * Represents the Real-time Java Virtual Machine (RTJVM). This extends
 * the {@link JavaVirtualMachine} class to specialize certain aspects for
 * use in a RTSJ configuration - such as dealing with no-heap threads and
 * scoped memory.
 * <p> {@inheritDoc}
 *
 * @author David Holmes
 */
public class RTJavaVirtualMachine extends JavaVirtualMachine {

  private static final boolean DEBUG_GCTHREAD = false;

    /* This is the entry point for the JVM at which time class initialization
       is disabled. Consequently this class can not have any static
       initialization requirements. At the time of writing, this class
       manages to escape static initialization until VM shutdown commences!

       The RealtimeLauncher invokes our init() method.
    */

    // we need to install our singleton instance as the JVM instance
    public static void init() {
        JavaVirtualMachine.init();
        // now replace the non-RT JVM instance with our RT one.
        ImmortalMemory.instance().executeInArea(new Runnable() {
                public void run() {
                    JavaVirtualMachine.instance = new RTJavaVirtualMachine();
                }
            });
    }


    /** Direct construction of a JVM is not allowed */
    RTJavaVirtualMachine() {}


    void gc() {
        if (Thread.currentThread() instanceof NoHeapRealtimeThread)
            throw new MemoryAccessError();

        super.gc();
    }



    /** 
     * The thread that executes the shutdown sequence.
     */
    ShutdownThread shutdown; 

    protected int getSystemPriority_() { return 40 /* FIXME, should probably have a better way of syncing with the constants in javax.realtime.RealtimeJavaDispatcher and in s3.services.java.realtime.RealtimeJavaDispatcherImpl */; }

    /** Overrides to start the shutdown thread */
    void initializeVMThreads() {
        ImmortalMemory.instance().executeInArea(new Runnable() {
                public void run() {
                    shutdown = new ShutdownThread();
                    // initialize the scope finalizer thread by forcing
                    // static init of its class. We can't access it directly.
                    try {
                        Class c = 
                            Class.forName("javax.realtime.ScopeFinalizerThread");
                    }
                    catch (Throwable t) {
                        // In a RT-config in which no RT classes are actually
                        // used - eg SPECJVM98 - the javax stuff won't be in
                        // the image and we will get an exception here.
                        //                        throw new InternalError("Couldn't load ScopeFinalizerThread class: " + t);
                    }
                }
            });
        shutdown.start();
        super.initializeVMThreads();
    }
    
    void initializeGCThreads() {
        
	if (LibraryImports.needsGCThread()) {
	    ImmortalMemory.instance().executeInArea(new Runnable() {
		    public void run() {

		        int prio = LibraryImports.getGCThreadPriority();		    
			System.out.println("Starting GC thread with priority "+prio+"...");		    
			new javax.realtime.RealtimeThread(new org.ovmj.java.UncheckedPriorityParameters(prio)) {
			    {
				setName("GC thread");
				setDaemon(true);
			    }
			    public void run() {
				LibraryImports.runGCThread();
			    }
			}.start();
			System.out.println("Started GC thread with priority "+prio+".");
		    }
		});
	}
    }

    /**
     * Performs the shutdown sequence and then halts the JVM. The
     * <tt>shutdownInitiated</tt> flag should already have been set
     * by the caller. The shutdown thread is notified, then the current
     * thread 'hangs' as it tries to join() on the shutdown thread.
     */
    void performShutdown() {
        synchronized(this) {
            notify();
        }

        // this should never return as the shutdown thread never actually
        // terminates
        while (true) {
            try {
                shutdown.join();
                throw new InternalError("shutdown.join() returned normally");
            }
            catch(InterruptedException ex) {
                // ignore - we're already 'terminated'
            }
        }
    }

    /**
     * Thread subclass for performing the VM shutdown sequence. The instance
     * of this class is allocated in immortal memory and waits until notified
     * that shutdown is being initiated.
     * This is a non-static nested class as it accesses the current
     * JVM object.
     */
    class ShutdownThread extends Thread {

        ShutdownThread() {
            this.setDaemon(true);
	    priority = LibraryImports.getMaximumPriority();
            this.setName("Shutdown-Thread");
        }

        public void run() {
            synchronized(RTJavaVirtualMachine.this) {
                while (!shutdownInitiated) {
                    try {
                        RTJavaVirtualMachine.this.wait();
                    }
                    catch(InterruptedException ex) {
                        // ignore
                    }
                }
            }
            // must not hold lock on JVM instance when doing shutdown
            // otherwise the hook threads we join() with can't be started or
            // terminate
            RTJavaVirtualMachine.super.performShutdown();
        }
    }
}








