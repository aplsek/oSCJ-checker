package javax.realtime;
import org.ovmj.java.Opaque;
/**
 * The <tt>RealtimeJavaDispatcher</tt> is to the {@link RealtimeThread} class
 * what the {@link java.lang.JavaDispatcher} is to the {@link Thread} class.
 * It provides a marshalling interface between the real-time classes and
 * the kernel services via the library import mechanism.
 * <p>Ideally, this class would extend <tt>JavaDispatcher</tt> and we would
 * simply configure the appropriate instance at runtime. However, the package
 * scoping mechanism prevents this unless we make <tt>JavaDispatcher</tt>
 * public - which we definitely do not want to do. Consequently, we have to
 * duplicate some of the functionality of the <tt>JavaDispatcher</tt>.
 *
 * @author David Holmes
 */
class RealtimeJavaDispatcher {

    /* a value we keep neeeding all over the place with times */
    static final int NANOS_PER_MILLI = 1000 * 1000;

    /* these are kernel constants that we need to understand */

    /** Absolute sleep caused thread to block */
    static final int ABSOLUTE_NORMAL = 0;
 
    /** Absolute sleep time was in the past */
    static final int ABSOLUTE_PAST = -1;
 
    /** Absolute sleep was interrupted */
     static final int ABSOLUTE_INTERRUPTED = 1;


    /** The singleton instance of this class */
    static final RealtimeJavaDispatcher instance = 
                                  new RealtimeJavaDispatcher();

    /**
     * Return the singleton instance of this class 
     * @return the singleton instance of this class 
     */
    static RealtimeJavaDispatcher getInstance() {
        return instance;
    }

    /** no construction allowed */
    private RealtimeJavaDispatcher() {}


    /** the minimum RT priority available for application threads */
    static final int MIN_RT_PRIORITY;

    /** the maximum RT priority available for application threads */
    static final int MAX_RT_PRIORITY;

    /** maximum RT priority allocated to internal VM threads */
    static final int SYSTEM_RT_PRIORITY;

    /** maximum non-RT priority allocated to internal VM threads */
    static final int SYSTEM_PRIORITY;

    static {
        // This is a hard-coded "hack". We know the underlying dispatcher
        // provides 40 priority values from 1 to 40, intended for use as
        // follows:
        // 1 - 10  Normal Java threads
        // 11      RESERVED for JVM
        // 12 - 39 Real-time threads
        // 40      RESERVED for JVM
        SYSTEM_PRIORITY = 11;
        MIN_RT_PRIORITY = 12;
        MAX_RT_PRIORITY = 39;
        SYSTEM_RT_PRIORITY = 40;
        Assert.check(LibraryImports.getMinRTPriority() == 1 &&
                     LibraryImports.getMaxRTPriority() == 42 ? Assert.OK :
                     "priority range mis-match");
    }

    /**
     * Put the current thread to sleep until the specified time has passed
     * or until the thread is interrupted.
     *
     * @param nanos the time to sleep until, expressed as nanoseconds since
     * the epoch.
     *
     * @return <tt>true</tt> if the thread actually slept, and
     * <tt>false</tt> if the specified time had already passed.
     * @throws InterruptedException if the thread was interrupted, in which
     * case the threads interrupt state is cleared.
     */
    boolean sleepAbsolute(long nanos) throws InterruptedException{
        int rc = LibraryImports.delayCurrentThreadAbsolute(nanos);
        switch (rc) {
            case ABSOLUTE_NORMAL: return true;
            case ABSOLUTE_PAST: return false;
            case ABSOLUTE_INTERRUPTED: {
                RealtimeThread.currentRealtimeThread().clearInterrupt();
                throw new InterruptedException();
            }
            default: throw new InternalError("invalid return code " + rc);
        }
    }

    /**
     * Put the current thread to sleep until the specified time has passed
     * or until the thread is interrupted, but throw no exception upon
     * interrupt.
     *
     * @param nanos the time to sleep until, expressed as nanoseconds since
     * the epoch.
     *
     * @return An integer code indicating why the call returned:
     *    ABSOLUTE_NORMAL means the sleep elapsed normally, ABSOLUTE_PAST
     *    means the sleep time had already passed, and ABSOLUTE_INTERRUPTED
     *    means an interrupt occurred.
     */
    int sleepAbsoluteRaw(long nanos) {
        return LibraryImports.delayCurrentThreadAbsolute(nanos);
    }

    /**
     * Starts the given thread at the given time.
     * @param t the thread to start
     * @param startTime the absolute start time in nanoseconds
     */
    void startThreadDelayed(RealtimeThread t, long startTime) {
        Opaque vmThread = LibraryImports.getVMThread(t);
        LibraryImports.startThreadDelayed(vmThread, startTime);
    }

    /**
     * Atomically queries the given thread to see if its scheduling priority
     * can be changed and, if allowed, sets the new priority.
     * The RTSJ V1.0 says this can only
     * happen if the thread is not alive, or if blocked in a sleep or
     * wait. We've changed this to allow changes at any time - which means
     * this method is redundant and we'll revise this soon.
     * <p>Note that the thread is locked while this occurs. This should only
     * be called if the thread is alive.
     * @param t the thread to be queried
     * @param newPrio the new priority of the thread
     *
     * @return <tt>true</tt> if the thread's scheduling parameters can be
     * set and the new priority has been set, and <tt>false</tt> otherwise.
     */
    boolean canSetSchedulingParameters(RealtimeThread t, int newPrio) {
        Opaque vmThread = LibraryImports.getVMThread(t);
        return LibraryImports.setPriorityIfAllowed(vmThread, newPrio);
    }


    /**
     * Put the current thread to sleep until the specified time has passed.
     *
     * @param nanos the time to sleep until, expressed as nanoseconds since
     * the epoch.
     *
     * @return <tt>true</tt> if the thread actually slept, and
     * <tt>false</tt> if the specified time had already passed.
     */
    boolean sleepAbsoluteUninterruptible(long nanos) {
        int rc = LibraryImports.delayCurrentThreadAbsoluteUninterruptible(nanos);
        switch (rc) {
            case ABSOLUTE_NORMAL: return true;
            case ABSOLUTE_PAST: return false;
            default: throw new InternalError("invalid return code " + rc);
        }
    }

    /**
     * Return the minimum real-time priority value for application threads.
     * @return the minimum real-time priority value.
     */
    int getMinRTPriority() {
        return MIN_RT_PRIORITY;
    }

    /**
     * Return the maximum real-time priority value for application threads.
     * @return the maximum real-time priority value.
     */
    int getMaxRTPriority() {
        return MAX_RT_PRIORITY;
    }

}






