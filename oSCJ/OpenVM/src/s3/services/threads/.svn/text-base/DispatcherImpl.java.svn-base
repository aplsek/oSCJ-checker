/* 
 * DispatcherImpl.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/s3/services/threads/DispatcherImpl.java,v 1.56 2006/04/20 15:48:42 baker29 Exp $
 *
 */
package s3.services.threads;

import ovm.core.domain.DomainDirectory;
import ovm.core.execution.Context;
import ovm.core.execution.Processor;
import ovm.core.execution.RuntimeExports;
import ovm.core.services.io.BasicIO;
import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.OVMThreadContext;
import ovm.core.stitcher.MonitorServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.services.ServiceInstanceImpl;
import ovm.services.monitors.Monitor;
import ovm.services.threads.UserLevelDispatcher;
import ovm.services.threads.UserLevelThreadManager;
import ovm.util.Iterator;
import ovm.util.OVMError;
import s3.util.PragmaInline;
import s3.util.PragmaNoPollcheck;
/**
 * A basic dispatcher implementation for the basic user-level thread manager
 * of the OVM.
 *
 * <p>All methods that disable rescheduling or which are only called with
 * rescheduling disabled, declare PragmaNoPollcheck.
 */
public class DispatcherImpl extends ServiceInstanceImpl
    implements UserLevelDispatcher {

    /** Our thread manager implementation */
    protected UserLevelThreadManager tm;

    /**
     * Number of threads that have been started but not yet terminated.
     * When this gets to zero in {@link #terminateCurrentThread} we exit
     * the OVM.
     *
     */
    protected volatile int nThreads = 0;

    /** The singleton instance of this class */
    final static OVMDispatcher instance = new DispatcherImpl();

    /**
     * Return the singleton instance of this class 
     * @return the singleton instance of this class 
     */
    public static OVMDispatcher getInstance() {
        return instance;
    }

    /**
     * Trivial no-arg constructor
     * @see #init
     */
    protected DispatcherImpl() {}

    public void init() {
        tm = (UserLevelThreadManager)((ThreadServicesFactory)ThreadServiceConfigurator.config.getServiceFactory(ThreadServicesFactory.name)).getThreadManager();
	isInited = true;
    }
    

    /* We inherit most of the interface defined doc comments */

    
    /** Flag to indicate if threading has been initialized */
    private boolean threadingInitialized = false;
    

    public final boolean threadingInitialized() throws PragmaInline {
        return threadingInitialized;
    }
    
    /**
     * {@inheritDoc}.
     *
     * <p>This is a final method to ensure correct initialization always
     * takes place. Customization of the initialization process is performed
     * via specific hooks.
     */
    public final void initializeThreading() {

        // make sure we're initialized
        if (!isInited()) {
            init();
        }
        // initialize the thread manager
        if (!tm.isInited()) {
            tm.init();
        }

        // double-check we're in the boot thread
        OVMThread boot = tm.getCurrentThread();
        if (boot != null) {
            d("Threading system appears to be already initialised");
            return;
        }

        // Bind the primordial thread to the initial boot context

        Context ctx = Processor.getCurrentProcessor().getContext();
        if (!(ctx instanceof OVMThreadContext)) 
            throw new OVMError.Configuration("Need OVMThreadContext");

        OVMThreadContext threadCtx = (OVMThreadContext) ctx;
        ThreadServicesFactory tsf = 
            (ThreadServicesFactory)ThreadServiceConfigurator.config.
            getServiceFactory(ThreadServicesFactory.name);
        OVMThread primThread = tsf.getPrimordialThread(threadCtx);
        // now bind the primordial thread to the current context so that we
        // have a notion of current thread.
        threadCtx.setThread(primThread);

        // activate the thread manager if needed
        if (!tm.isStarted()) {
            tm.start();
        }

        // perform dispatcher specific initialization of the primordial thread
        initPrimordialThread(primThread);

        // "start" the primordial thread
        startThread(primThread);
        
        // now finish registration with the thread manager
        tm.registerCurrentThread();

        assert tm.getCurrentThread() == primThread:
	    "current thread not primordial thread";

        assert tm.isReschedulingEnabled(): "rescheduling not enabled";

        // now do any late monitor initialization
        MonitorServicesFactory msf = (MonitorServicesFactory)ThreadServiceConfigurator.config.getServiceFactory(MonitorServicesFactory.name);
        Monitor.Factory factory = msf.getMonitorFactory();
        factory.initialize();

        d("[Threading Initialization] Done. Synchronization support is NOT enabled. - SCJ.");
        
        // tell the CSA to process MONITORENTER/EXIT bytecodes
        //DomainDirectory.getExecutiveDomain().
        //    getCoreServicesAccess().enableSynchronization();

        threadingInitialized = true;
    }


    public void afterContextSwitch() throws PragmaInline {
        // nothing special here
    }

    public OVMThread getCurrentThread() throws PragmaInline {
        return tm.getCurrentThread();
    }

    public void yieldCurrentThread()  throws PragmaNoPollcheck {
        OVMThread current = tm.getCurrentThread();
        boolean enabled = tm.setReschedulingEnabled(false);
        try {
            tm.removeReady(current);
            tm.makeReady(current);
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }
    }

    public void startThread(OVMThread thread)  throws PragmaNoPollcheck {
        boolean enabled = tm.setReschedulingEnabled(false);
        try {
            nThreads++;
            tm.makeReady(thread);
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }
    }


    /** 
     * Terminates the current thread. This method never returns and must never
     * throw an exception.
     * <p>When the last started thread terminates, the OVM terminates.
     * <p>This method should be called as part of thread termination.
    */
    public void terminateCurrentThread() throws PragmaInline {
        terminateCurrentThread(tm.getCurrentThread());
    }

    /**
     * Terminates the given thread, which is assumed to be the current
     * thread.
     * <p>This is a utility method for subclasses to call so we don't
     * duplicate the getCurrentThread call.
     */
    protected void terminateCurrentThread(OVMThread current)  
        throws PragmaNoPollcheck{
        boolean enabled = tm.setReschedulingEnabled(false);
        try {
            onTermination(current); // termination hook

            // assert: current thread holds no locks
            if (--nThreads == 0) {
                d("OVM Terminating due to last thread termination");
                ovm.core.Executive.shutdown();             
	    }
            tm.removeReady(current);
            tm.destroyThread(current); // will destroy when safe
            tm.runNextThread();  // should never return
            throw new OVMError.Internal("terminated thread got rescheduled");
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }
    }


    /**
     * Hook to allow dispatcher specific actions on termination of a thread.
     * At the time this is called the thread is still the current thread and
     * rescheduling is disabled.
     *
     * @param current the current thread which is terminating
     */
    protected void onTermination(OVMThread current)  throws PragmaInline {}

    public int activeThreads() throws PragmaInline{
        return nThreads;
    }


    /**
     * Customization hook to allow a dispatcher to perform custom
     * initialization of the primordial thread object. For example, to
     * set priority. No assumptions should be made as to the initialization
     * state of the threading system.
     * @param primordialThread the thread object that will be attached to
     * the primordial thread of execution.
     */
    protected void initPrimordialThread(OVMThread primordialThread) 
        throws PragmaInline { 
    }



    /**
     * Returns an iterator over all threads in the system that have been
     * created but have not yet terminated. This utilises the Context iterator
     * and so can not distinguish between started and non-started threads.
     */
    public Iterator iterator() {
        return new Iterator() {
                Iterator iter = Context.iterator();
                
                public boolean hasNext() {
                    return iter.hasNext();
                }
                 public Object next() {
                     return ((OVMThreadContext) iter.next()).getThread();
                 }
                public void remove() {
                     throw new OVMError.UnsupportedOperation();
                }
            };
    }

    /**
     * {@inheritDoc}.
     * <p>This method should only be called with rescheduling disabled.
     */
    public void dumpStacks()  throws PragmaNoPollcheck {
        Throwable e = new Throwable();
        String unavail = " <UNAVAILABLE>";

        OVMThread current = tm.getCurrentThread();
        BasicIO.err.print("## Stack trace for current thread " + current);

        RuntimeExports rte =
	    DomainDirectory.getExecutiveDomain().getRuntimeExports();
	Throwable throwable = new Throwable();
        StackTraceElement[] stack = throwable.getStackTrace();
	
        if (stack == null) {
            BasicIO.err.println(unavail);
        }
        else {
            BasicIO.err.println();
            for(int i = 0; i < stack.length; i++) {
                BasicIO.err.print("   " + i + ": ");
                BasicIO.err.println(stack[i].toString());
            }
        }

        for (Iterator iter = iterator(); iter.hasNext(); ) {
            OVMThread t = (OVMThread) iter.next();
            if (t == current) {
                continue;
            }
            BasicIO.err.print("## Stack trace for thread " + t);
            stack = (StackTraceElement[]) (Object)
		rte.getStackTrace(asOop(throwable), asOop(t.getContext()));
            if (stack == null) {
                BasicIO.err.println(unavail);
            }
            else {
                BasicIO.err.println();
                for(int i = 0; i < stack.length; i++) {
                    BasicIO.err.print("   " + i + ": ");
                    BasicIO.err.println(stack[i].toString());
                }
            }
        }
    }
}











