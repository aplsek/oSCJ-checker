
package ovm.services.io.async;

/**
 * Generic interface for async callbacks.
 * @author Filip Pizlo
 */
public interface AsyncCallback {
    /**
     * This method gets called when the Async IO layer thinks that the
     * operation is almost complete.  It is your job to then call
     * <code>AsyncFinalizer.finish()</code> in the most applicable
     * scheduling context (for example, if you're implementing blocking
     * IO, then you should unblock that thread and have it call
     * <code>finish()</code>).  Note that this method may be called either from
     * interrupt context with rescheduling disabled, or from the context of
     * any thread using the same resource that you are, in which case
     * rescheduling may be enabled.  This means that you must not allocate
     * memory in the implementation of this method.
     * Another invariant that must hold is that this method must not call
     * finish() - it can only arrange for finish() to be called.
     * @param finalizer the <code>AsyncFinalizer</code> that you're supposed
     *                  to call into to finish the operation.
     */
    public void ready(AsyncFinalizer finalizer);
    
    /**
     * Returns the priority that the request for this callback should
     * be processed.  Some implementations may ignore this and do
     * FIFO processing not matter what - but this is all yet to be
     * resolved.
     * <p>
     * One thing that you may be asking yourself is: why this method
     * here?  Is there no better way of telling the IO system the
     * priority of requests?  The reason why we put it here is simple.
     * The whole reason why there is one unified AsyncCallback interface,
     * as opposed to a bunch of separate ones that don't have anything
     * in common except coincidentally similar-looking method names, is
     * to make blocking IO easier to implement.  You see, async IO is
     * fine and good in the kernel, and gives us plenty of power, but
     * ultimately what Java and most other thingies demand is that the
     * green threads block on IO without blocking the VM.  So, how does
     * one implement blocking IO on top of async IO?  Certainly, one
     * possibility would be to effectively special-case each operation,
     * and implement blocking semantics for it separately.  But this
     * would mean something like 20 dense, error-prone lines of code
     * repeated for each syscall.  That's bad.  So, the solution is
     * to have the 'blocking IO layer' implement a stock AsyncCallback
     * implementation that only does the three methods in here.
     * Implementing individual syscalls in the blocking IO layer would
     * mean, among other things, extending this one class to give it
     * the one (or more) methods that are required by that syscall's
     * async callback interface.
     * <p>
     * So, the idea of the stock AsyncCallback implementation would be
     * to capture as much commonality between syscalls as possible, and
     * to provide the bulk of the blocking IO semantics within the OVM.
     * Naturally, this includes thread priorities.  So, thread priorities
     * made it into this interface because it makes the implementation
     * more natural.
     */
    public int getPriority();
    
    public static class Delegate implements AsyncCallback {
        private AsyncCallback target_;
        
        public Delegate(AsyncCallback target) {
            this.target_=target;
        }
        
        public AsyncCallback getTarget() {
            return target_;
        }
        
        public void ready(AsyncFinalizer fin) {
            getTarget().ready(fin);
        }
        
        public int getPriority() {
            return getTarget().getPriority();
        }
    }
}

