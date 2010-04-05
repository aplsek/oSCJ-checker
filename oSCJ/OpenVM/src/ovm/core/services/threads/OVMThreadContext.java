package ovm.core.services.threads;

import ovm.core.execution.Context;
import ovm.util.OVMError;
/**
 * A specialised {@link Context execution context} that keeps track of it's
 * associated thread as &quot;context-local-storage&quot;.
 *
 * @author David Holmes
 *
 */
public class OVMThreadContext extends Context {

    /** 
     * The thread associated with this context. If this is the current
     * context, then this is the current thread. Used by the
     * {@link ThreadManagerCoreImpl thread manager}.
     */
    /*package*/
    volatile OVMThread thisThread;

    /** per-thread errno value from the underlying system */
    private int errno_;
    
    /** per-thread h_errno value from the host lookup code */
    private int h_errno_;
    
    /**
     * Create an unbound thread context.
     * Needed to initialise the primordial thread context
     *
     */
    protected OVMThreadContext() {
        this(null);
    }

    /**
     * Create a new thread context bound to the given thread
     *
     * @param thread the thread to bind the context to.
     *
     */
    protected OVMThreadContext(OVMThread thread) {
        thisThread = thread;
    }

    /**
     * Sets the thread bound to this context.
     * @param thread the thread to bind
     * @throws OVMError.IllegalState if already bound
     */
    public void setThread(OVMThread thread) {
        if (thisThread != null) {
            throw new OVMError.IllegalState("already bound");
        }
        thisThread = thread;
    }

    /**
     * Returns the thread bound to this execution context
     * @return  the thread bound to this execution context
     *
     */
    public OVMThread getThread() {
        return thisThread;
    }

    /** Return the last set errno value for this thread */
    public int getErrno() { return errno_; }

    /** Set errno for this thread to the given value */
    public void setErrno(int errno) { errno_ = errno; }

    /** Return the last set h_errno value for this thread */
    public int getHErrno() { return h_errno_; }

    /** Set h_errno for this thread to the given value */
    public void setHErrno(int h_errno) { h_errno_ = h_errno; }

    public String toString() {
        return super.toString() + " bound to thread: " +  thisThread;
    }

    public static class Factory extends Context.Factory {
	public Context make() {
	    return new OVMThreadContext();
	}
	public OVMThreadContext make(OVMThread thisThread) {
	    return new OVMThreadContext(thisThread);
	}
    }

    public static Factory threadContextfactory()
	// throws PragmaRefineSingleton FIXME: see bug #546
    {
	return (Factory) Context.factory();
    }
}
