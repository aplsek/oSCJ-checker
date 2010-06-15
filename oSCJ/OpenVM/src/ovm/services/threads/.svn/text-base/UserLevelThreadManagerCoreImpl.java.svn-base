/**
 * UserLevelThreadManagerCoreImpl.java
 * Created on January 28, 2002, 10:25am
 */
package ovm.services.threads;

import ovm.core.execution.Context;
import ovm.core.execution.Processor;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.ThreadManagerCoreImpl;
import ovm.core.stitcher.ThreadDispatchServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.util.OVMError;
import s3.services.java.ulv1.JavaMonitorImpl;
import s3.services.transactions.Transaction;
import s3.util.PragmaInline;
import s3.util.PragmaNoPollcheck;

/**
 * A concrete implementation of {@link UserLevelThreadManager} which provides
 * the low-level context switch whose
 * implementation is intrinsic to the organisation of the OVM itself.
 * A more specific thread manager implementation can subclass this class to
 * acquire
 * this method without having to understand the internal OVM representation
 * and structure of things - like code fragments, contexts, activations etc.
 * <p>All methods that disable rescheduling or which are only called with
 * rescheduling disabled, declare PragmaNoPollcheck.
 *
 * @author David Holmes
 */
public abstract class UserLevelThreadManagerCoreImpl
    extends ThreadManagerCoreImpl
    implements UserLevelThreadManager {

    // the user-level dispatcher we're working with
    static UserLevelDispatcher dispatcher;

    public void init() {
        try {
            dispatcher = (UserLevelDispatcher)((ThreadDispatchServicesFactory)ThreadServiceConfigurator
        	    	.config.getServiceFactory(ThreadDispatchServicesFactory.name)).getThreadDispatcher();
            if (dispatcher == null)
                throw new OVMError.Configuration("no dispatcher");
            isInited = true;
        }
        catch (ClassCastException ex) {
            throw new OVMError.Configuration("wrong dispatcher configuration");
        }
    }


    /** Reference to a &quot;dead&quot; context that needs to be destroyed */
    private volatile Context deadContext;

    // setter method to identify a scoped access
    private final void setDeadContext(Context ctx) 
        throws PragmaNoBarriers, PragmaInline {
        deadContext = ctx;
    }

    /** 
     * A reference to the current thread. This is an optimisation for the
     * user-level thread manager because we know there is only a single
     * thread current at a time so we don't need to get the current 
     * processor etc. If we ever support more than one real thread then this
     * will need to be changed.
     */
    private volatile OVMThread currentThread = null;

    // setter method to identify a scoped access
    private final void setCurrentThread(OVMThread t) 
        throws PragmaNoBarriers, PragmaInline {
        currentThread = t;
    }
        

    // inline elides pollcheck
    public final OVMThread getCurrentThread() 
        throws PragmaInline {
        return currentThread;
    }

    /**
     * Context switches to the specified thread. The current thread is
     * no longer active and will remain inactive until it becomes
     * the parameter to this method from another thread. After control
     * returns to the current thread, we invoke the dispatcher's
     * {@link UserLevelDispatcher#afterContextSwitch afterContextSwitch} hook.
     *
     * @param t the thread to switch to. If this thread has never executed then
     * execution commences in its {@link OVMThread#runThread} method, otherwise
     * control must be within the current method and so we resume execution
     * for this thread in the current method.
     *
     */
    protected final void runThread(OVMThread t) throws s3.util.PragmaNoPollcheck {
	if (JavaMonitorImpl.GATHER_MONITOR_STATISTICS) {
	    //BasicIO.out.println("Switching from " + currentThread + " to " + t);
	    JavaMonitorImpl.M_SWITCH++;
	}
	boolean aborting = Transaction.the().preRunThreadHook(currentThread, t);
        setCurrentThread(t);
        Processor.getCurrentProcessor().run(t.getContext());

        if (deadContext != null) {
            deadContext.destroy();
            setDeadContext(null);
        }

	Transaction.the().postRunThreadHook(aborting); 

 	dispatcher.afterContextSwitch();

	Context.doContextDebug("after context switch");
    }

    public final void destroyThread(OVMThread t) 
        throws PragmaNoBarriers, PragmaNoPollcheck  {
	assert !isReady(t): "destroyed thread still in run queue";
        if (deadContext != null) {
            // this can only happen if the last thread to die switched
            // to a brand new thread (t) which executed to completion 
            // without any intervening context switch causing it to execute 
            // runThread
            deadContext.destroy();
        }
        setDeadContext(t.getContext());
    }

    /**
     * Initializes the given thread to work with the current thread
     * manager. The minimum requirement is that rescheduling is enabled.
     * <p>Subclasses may override to perform additional initialization or
     * checking, but they should ensure that this method (or equivalent
     * functionality) is always invoked.
     */
    public void registerCurrentThread() {
        // this is a way to get currentThread set for the primordial thread
        if (currentThread == null)
            setCurrentThread(super.getCurrentThread());
        setReschedulingEnabled(true);
    }
}








