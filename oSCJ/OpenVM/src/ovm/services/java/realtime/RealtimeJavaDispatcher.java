package ovm.services.java.realtime;

import ovm.core.domain.Oop;
import ovm.services.java.JavaDispatcher;
import ovm.services.realtime.RealtimePriorityDispatcher;
import ovm.services.java.JavaOVMThread;

/** 
 * The <tt>RealtimeJavaDispatcher</tt> extends the {@link JavaDispatcher} to
 * provide methods and semantics that support the implementation of the
 * <a href="http://www.rtj.org">Realtime Specification for Java</a> as
 * defined by the classes in the <tt>javax.realtime</tt> package, by
 * combining with the {@link RealtimePriorityDispatcher} interface.
 * We also provide new methods for doing absolute sleeps and uninterruptible
 * sleeps (used for periodic releases).
 *
 * @author David Holmes
 */
public interface RealtimeJavaDispatcher 
    extends JavaDispatcher, 
    RealtimePriorityDispatcher 
{

    // These are ugly but we need a ternary return value for an
    // interruptible, absolute sleep.

    /** Absolute sleep caused thread to block */
    public static final int ABSOLUTE_NORMAL = 0;

    /** Absolute sleep time was in the past */
    public static final int ABSOLUTE_PAST = -1;

    /** Absolute sleep was interrupted */

    public static final int ABSOLUTE_INTERRUPTED = 1;

    /** 
     * Blocks the current thread until the specified delay has elapsed.
     * <p>This is equivalent to {@link JavaDispatcher#delayCurrentThread}
     * except that an interruption will not wake the thread.
     *
     * @param millis the number of milliseconds to delay for
     * @param nanos the additional number of nanoseconds to delay for
     *
     */
    public void delayCurrentThreadUninterruptible(long millis, int nanos);


    /**
     * Blocks the current thread until the specified absolute point in time,
     * or until it is interrupted.
     *
     * @param wakeupTime the absolute time, measured in nanoseconds since
     * the EPOCH, when the current thread should wakeup from its sleep.
     * @return <tt>ABSOLUTE_NORMAL</tt> if the wakeup time was in the future
     * and the thread actually blocked; <tt>ABSOLUTE_PAST</tt> if the wakeup
     * time had already passed and the thread did not block; or 
     * <tt>ABSOLUTE_INTERRUPTED</tt> if the thread was interrupted at the time
     * it made this call, or whilst the thread was blocked.
     */
    public int delayCurrentThreadAbsolute(long wakeupTime);

    /**
     * Blocks the current thread until the specified absolute point in time.
     * <p>This is equivalent to <tt>delayCurrentThreadAbsolute</tt> except
     * that an interruption will not wake the thread.
     *
     * @param wakeupTime the absolute time, measured in nanoseconds since
     * the EPOCH, when the current thread should wakeup from its sleep.
     * @return <tt>ABSOLUTE_NORMAL</tt> if the wakeup time was in the future
     * and the thread actually blocked, or <tt>ABSOLUTE_PAST</tt> if the wakeup
     * time had already passed and the thread did not block.
     */
    public int delayCurrentThreadAbsoluteUninterruptible(long wakeupTime);


    /**
     * Creates a new VM thread that is associated with the given Java
     * thread, with the given heap affinity. A call to
     * <tt>createVMThread(javaThread) is equivalent to calling 
     * <tt>createVMThread(javaThread, false)</tt>
     * @param javaThread the Java thread to which the new thread should be
     * bound.
     * @param noHeap If <tt>true</tt> then the VM thread must correspond to
     * a no-heap thread and not be permitted access to the heap.
     * @return A reference to a newly created, but not started, VM thread
     * that is associated with the given Java thread.
     *
     */
    public JavaOVMThread createVMThread(Oop javaThread, boolean noHeap);

    public boolean setPriorityIfAllowed(JavaOVMThread th, int newPrio);
}



