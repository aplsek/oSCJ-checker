/*
 * ThreadManager.java
 *
 * Created 30 November, 2001 09:25
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/core/services/threads/ThreadManager.java,v 1.9 2004/02/20 08:48:08 jthomas Exp $
 */
package ovm.core.services.threads;


/**
 * A thread manager provides the means for control and manipulating threads
 * of control within the OVM. This base <code>ThreadManager</code>
 * interface defines those methods common to all thread managers, regardless
 * of the actual threading model being implemented (user-level, native, or 
 * some hybrid).
 * <p>A thread manager does not provide the high-level API normally associated
 * with threads, but rather the lower-level API needed to implement those 
 * higher level methods. This difference is most noticeable in 
 * {@link ovm.services.threads.UserLevelThreadManager user-level thread managers} where the
 * low-level interface is one of queue manipulation. It is least noticeable in
 * native thread managers where there may be a direct correspondence between
 * the programming thread API and the native thread API (such as POSIX
 * Pthreads). A language specific dispatcher usually provides the mapping
 * between the language level threading API and the thread manager in use.
 *
 * <p>Extensions to <code>ThreadManager</code> will
 * provide additional thread management functionality suitable for the actual
 * threading model they support.
 * <p> A fully functional thread manager is likely to implement several 
 * different thread manager interfaces.
 *
 * @see ovm.services.threads.UserLevelThreadManager
 * @see ovm.services.java.JavaDispatcher
 */
public interface ThreadManager extends ovm.services.ServiceInstance {

    /**
     * Returns the currently  executing <code>OVMThread</code> instance
     * @return the currently executing <code>OVMThread</code> instance
     *
     */
    OVMThread getCurrentThread();

    /**
     * Allows for the registration and initialization of the current thread 
     * by the thread manager. This method should be called once for each thread
     * as the very first thing it does when it begins execution.
     * The primordial thread is exempt from this requirement.
     * <p>For example, this might be needed to test whether the thread is
     * of the right type for the currently configured thread manager; or
     * it might enabling rescheduling in a user-level thread manager, and
     * so on.
     */
    void registerCurrentThread();
}








