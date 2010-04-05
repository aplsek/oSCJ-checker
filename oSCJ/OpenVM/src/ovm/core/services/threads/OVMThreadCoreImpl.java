/*
 * OVMThreadCoreImpl.java
 *
 * Created on January 28, 2002, 9:42am
 */
package ovm.core.services.threads;

import ovm.core.domain.DomainDirectory;
import ovm.core.domain.ReflectiveMethod;
import ovm.core.execution.InvocationMessage;
import ovm.core.execution.Native;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Address;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.util.OVMError;
import s3.util.PragmaInline;

/**
 * A concrete implementation of {@link OVMThread} which provides those methods
 * whose implementation are intrinsic to the organisation of the OVM itself.
 * A more specific thread implementation can subclass this class to acquire
 * these methods without having to understand the internal OVM representation
 * and structure of things - like code snippets, contexts, frames etc.
 * <p>This class is not thread-safe, instances should be created and configured
 * by a single thread (usually their parent).
 *
 *
 * @author David Holmes
 */
public abstract class OVMThreadCoreImpl extends ovm.core.OVMBase
    implements OVMThread {

    /** The context in which this thread executes */
    OVMThreadContext ctx;

    volatile boolean interruptHandler = false;

    public boolean getInterruptHandlerFlag() {
      return interruptHandler;
    }
    
    public boolean setInterruptHandlerFlag(boolean newValue) {
    
      boolean oldValue = interruptHandler;
      interruptHandler = newValue;
//      Native.print_string("Setting IH flag to "+newValue+"\n");
      return oldValue;
    }

    private static final ReflectiveMethod runThread
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Lovm/core/services/threads/OVMThreadCoreImpl;",
			       "runThread:()V");


    /**
     * Create an OVM thread using the given execution context. Typically
     * this is used to initialize the primordial thread.
     *
     * @param ctx the execution context to associate with this thread
     */
    protected OVMThreadCoreImpl(OVMThreadContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Create an OVM thread with an execution context that will cause the
     * {@link #runThread} method of this thread to be executed, if the 
     * context is made the current context by the 
     * {@link ovm.core.execution.Interpreter Interpreter}.
     *
     */
    protected OVMThreadCoreImpl() {
        ctx = OVMThreadContext.threadContextfactory().make(this);
        // initialize context so the first activation invokes the run method
        InvocationMessage msg = runThread.makeMessage();
        // must pass this pointer to run method
        msg.setReceiver(VM_Address.fromObject(this).asOop());
        ctx.initialize(msg);
    }


    /**
     * Set the execution context associated with this thread, destroying the
     * existing context if any.
     * 
     * @param ctx the new execution context for this thread
     */
    public void setContext(OVMThreadContext ctx) {
        if (ctx == null) {
            throw new OVMError.IllegalArgument("null context");
        }
        if (this.ctx != null)
            this.ctx.destroy(); // must clean up!
        this.ctx = ctx;
    }

    public final OVMThreadContext getContext() throws PragmaInline {
        return this.ctx;
    }

    /**
     * The entry point for the execution of a thread. When a thread is
     * created its context is initialized such that switching to that context
     * will cause the execution of this method. Subclasses may not override
     * this method, but should instead override {@link #doRun} to perform the
     * required actions for that thread. 
     * <p>If {@link #doRun} ever returns then it is a fatal error and we halt
     * the interpreter.
     *
     */
    public final void runThread() {
        // this must be first as per its contract
	MemoryManager.the().observeContextSwitch(this);

	
//            BasicIO.err.print("runThread started\n");
        // if this throws an exception we're hosed. If we catch the exception
        // we don't know what to do with it and we can't terminate this thread
        // because we don't know how. - DH
        ((ThreadServicesFactory)ThreadServiceConfigurator.config.getServiceFactory(ThreadServicesFactory.name)).getThreadManager().registerCurrentThread();
//            BasicIO.err.print("Registered current thread - about to doRun()\n");
        try {
            prepareForRun();
            //            BasicIO.out.println(this.toString() + " - Initial allocation context is " + MemoryManager.the().getCurrentArea() );
            doRun();
            BasicIO.err.print("Aborting due to return of OVMThreadCoreImpl.doRun\n");
        }
        catch(Throwable t) {
            ovm.core.services.memory.MemoryPolicy.the().enterExceptionSafeArea();
            try { // can't let any exceptions escape
                if (t instanceof OutOfMemoryError) {
                    BasicIO.err.print("Aborting due to OutOfMemoryError "
                                      + " escaping OVMThreadCoreImpl.doRun: ");
                }
                else {
                    BasicIO.err.print("Aborting due to Uncaught exception "
                                      + " escaping OVMThreadCoreImpl.doRun: ");
                    try {
                        BasicIO.err.println(t);
                    }
                    catch(OutOfMemoryError oome) {
                        BasicIO.err.println("OutOfMemoryError doing toString on exception");
                    }
                    catch(Throwable t2) {
                        BasicIO.err.println("Unable to do toString on exception");
                    }
                    
                    try {
                        t.printStackTrace();
                    }
                    catch(Throwable t2) {
                        BasicIO.err.println("Unable to produce stack trace");
                    }
                }
            }
            catch(Throwable t3) {
                try { // still can't let anything escape
                    BasicIO.err.print("Nested exception occurred in " + 
				      "OVMThreadCoreImpl.run()");
                } 
                catch(Throwable t4) { // give up trying to report the problem
                }
            }
        }
        finally { // terminate
            try {  
                Native.exit_process(-1);   
	    } 
            catch(Throwable t5) {
                // give up altogether
            }
        }
    }

    /**
     * This method should be overridden by subclasses to provide the
     * specialised behaviour of this thread. It will be invoked from the
     * {@link #runThread} method when this thread begins executing when its 
     * context is switched to.
     * <p><b>This method should never return, either normally or exceptionally.
     * </b>.
     * <p>At this level the thread does not know enough about its execution
     * environment to know how to terminate cleanly - that must be done by the
     * subclass. If this method returns then the execution of the OVM will be
     * aborted.
     *
     */
    protected abstract void doRun();


    /**
     * This method should be overridden by subclasses to provide any special
     * set up that is needed for executing <tt>doRun()</tt>. For example, 
     * setting the initial allocation context. It is called immediately before
     * <tt>doRun</tt>.
     * <p>The default implementation does nothing.
     */
    protected void prepareForRun() {
    }
    
}// end of OVMThreadCoreImpl






