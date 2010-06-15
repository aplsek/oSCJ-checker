/*
 * $Header: /p/sss/cvs/OpenVM/src/ovm/core/services/threads/OVMDispatcher.java,v 1.10 2004/03/09 01:53:18 dholmes Exp $
 */
package ovm.core.services.threads;

/**
 * The dispatcher provides a higher-level threading API above that of the
 * raw {@link ThreadManager}. It will interact with other OVM services as
 * needed to achieve the required threading semantics.
 * <p>The raw threading subsystem is initialized using the dispatcher.
 * The boot &quot;thread&quot; should invoke {@link #initializeThreading} to
 * define the primordial thread and to register that thread with the thread
 * manager. Thereafter the main thread has no special role compared to other
 * threads. The OVM will terminate when the last started thread invokes
 * {@link #terminateCurrentThread}.
 *
 * @author David Holmes
 *
 */
public interface OVMDispatcher extends ovm.services.ServiceInstance {

    /**
     * Invoked by the boot &quot;thread&quot; to initialize the threading
     * subsystem and make the boot thread appear as a real thread.
     *
     * @throws ovm.util.OVMError.IllegalState if the invoking thread is not 
     * the boot thread, or has already been initialized.
     */
    public void initializeThreading();


    /**
     * Queries if the threading subsystem has been initialized.
     *
     * @return <tt>true</tt> if the threading system has been initialized and
     * <tt>false</tt> otherwise.
     */
    public boolean threadingInitialized();

    /** 
     * Obtain a reference to the currently executing thread.
     * @return a reference to the {@link OVMThread} object corresponding
     * to the currently executing thread.
     */
    public OVMThread getCurrentThread();

    /** 
     * Suggests that the currently executing thread ceases execution in
     * favour of some other thread that is ready to execute. Exactly what
     * this method will do depends on the implementation.
     */
    public void yieldCurrentThread();


    /**
     * Causes the specified thread to become eligible for execution.
     * When the current scheduling policy dictates, the context of the
     * specified thread can be made the current context and the thread
     * will commence execution in the method defined by that context.
     *
     * @param thread the thread to make eligible for execution
     *
     */
    public void startThread(OVMThread thread);

    /**
     * Terminates the execution of the current thread. This method
     * never returns.
     * <p>When the last started thread terminates, the OVM terminates.
     *
     */
    public void terminateCurrentThread();


    /**
     * Return the number of threads that this dispatcher has started, but which
     * have not yet terminated
     * @return the number of threads this dispatcher has started, but which
     * have not yet terminated
     */
    public int activeThreads();

     /**
      * Return an {@link ovm.util.Iterator iterator} for accessing all threads
      * in the system that have started but not yet terminated. This iterator
      * does not support the {@link ovm.util.Iterator#remove remove} method. 
      * @return an iterator to access all the threads in the system.
      */
    public ovm.util.Iterator iterator();


    /**
     * Print a stack trace for all threads in the system onto the standard 
     * error stream
     */
    public void dumpStacks();
}



