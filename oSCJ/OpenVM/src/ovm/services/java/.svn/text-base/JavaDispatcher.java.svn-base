/* 
 * JavaDispatcher.java
 *
 * Created 10 December, 2001 15:15
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/java/JavaDispatcher.java,v 1.19 2007/05/18 17:42:40 baker29 Exp $
 *
 */
package ovm.services.java;

import ovm.core.domain.Oop;
import ovm.services.threads.PriorityOVMDispatcher;
/** 
 * The <tt>JavaDispatcher</tt> extends the {@link PriorityOVMDispatcher} to
 * provide methods and semantics that support the {@link java.lang.Thread} 
 * API. The dispatcher will achieve this using the appropriate methods of
 * the thread manager.
 *
 * <h3>Thread safety</h3>
 * <p>Dispatcher methods should generally be invoked on threads that have been 
 * started (with the obvious exception of {@link #startThread}, 
 * {@link #isAlive}). The 
 * dispatcher may rely on the implementation of {@link java.lang.Thread} 
 * to ensure that a thread can not terminate while the dispatcher is trying 
 * to manipulate it - which is trivially the case when the dispatcher method 
 * operates on the current thread. In some cases, however, it may be difficult
 * for the thread implementation to guarantee this is the case, and so the 
 * dispatcher implementation should be aware of this possibility. 
 * Note, however, that simple locking schemes could lead to 
 * undesirable scheduling behaviour such as priority inversion. 
 * <p>The dispatcher may raise an {@link ovm.util.OVMError.IllegalState} 
 * exception if 
 * it determines that a thread has already terminated or has not been started.
 * The implementation must document when this is the case.
 *
 * @author David Holmes
 */
public interface JavaDispatcher extends PriorityOVMDispatcher{

    /** 
     * Returns a reference (as an opaque type) to the 
     * {@link Thread Java Thread} associated with the currently executing
     * VM thread.
     * @return a reference to the currently executing Java thread
     */
    public Oop getCurrentJavaThread();

    /** 
     * Returns a reference (as an opaque type) to the  currently executing
     * VM thread.
     * @return a reference to the currently executing VM thread.
     */
    public JavaOVMThread  getCurrentVMThread();


    /**
     * Creates a new VM thread that is associated with the given Java
     * thread.
     * @param javaThread the Java thread to which the new thread should be
     * bound.
     * @return A reference to a newly created, but not started, VM thread
     * that is associated with the given Java thread.
     *
     */
    public JavaOVMThread createVMThread(Oop javaThread);

    /** 
     * Blocks the current thread until the specified time has elapsed or the 
     * thread has been interrupted.
     * @param millis the number of milliseconds to delay for
     * @param nanos the additional number of nanoseconds to delay for
     *
     * @return <code>true</code> if the call returned due to the specified 
     * time elapsing, and <code>false</code> if the thread was interrupted.
     *
     * @see #interruptThread
     */
    public boolean delayCurrentThread(long millis, int nanos);

    /**
     * Queries whether the specified thread has started executing 
     * and has not yet terminated.
     *
     * @param vmThread The thread that is being queried     
     * @return <code>true</code> if <code>thread</code> has been the target 
     * of a call to {@link #startThread} (or is the primordial thread) but 
     * has not yet invoked {@link #terminateCurrentThread};
     * and <code>false</code> otherwise.
     *
     */
    public boolean isAlive(JavaOVMThread vmThread);


    /** 
     * Implements the actual mechanics needed to get a blocked thread to
     * respond to a {@link java.lang.Thread#interrupt} request.
     * <p> This method is not responsible for maintaining the thread state 
     * as returned by the {@link Thread#isInterrupted} method.
     *
     * @param vmThread The thread to be interrupted.
     * @see java.lang.Thread#interrupt
     */
     public void interruptThread(JavaOVMThread vmThread);

//      /** 
//       * Destroys the specified thread as per the 
//       * {@link java.lang.Thread#destroy} method. 
//       * As with that method, this method may not be implemented.
//       *
//       * @param vmThread The thread to be destroyed.
//       * @see java.lang.Thread#destroy
//       */
//      public void destroyThread(JavaOVMThread vmThread);

//      /** 
//       * Induces an asynchronous exception into the specified thread as per 
//       * the {@link java.lang.Thread#stop(Throwable)} method.
//       * <p><b>NOTE:</b> An implementation must ensure that it meets the 
//       * requirements of the Java Virtual Machine Specification (JVMS) and
//       * Java Language Specification (JLS) with regard to throwing
//       * asynchronous exceptions and maintaining the precise nature of Java
//       * exceptions.
//       * @param vmThread The thread to get the asynchronous exception
//       * @param exc the exception object to be thrown
//       *
//       * @see java.lang.Thread#stop(Throwable)
//       */
//      public void stopThread(JavaOVMThread vmThread, Throwable exc);


    /**
     * Binds the current VM thread (which is assumed to be the
     * primordial thread in the Java Virtual Machine) to the given
     * {@link java.lang.Thread} instance (passed as an opaque type).
     * 
     * @param javaThread the Java thread instance to bind to
     *
     * @throws OVMError.IllegalState if the currnet VM thread is already
     * bound to a Java thread
     */
    public void bindPrimordialJavaThread(Oop javaThread);
    
}







