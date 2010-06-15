/**
 * $Header: /p/sss/cvs/OpenVM/src/s3/services/threads/JLThread.java,v 1.38 2007/06/03 01:25:47 baker29 Exp $
 */
package s3.services.threads;

import ovm.core.services.io.BasicIO;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.ThreadManager;
import ovm.core.services.threads.OVMThreadContext;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.services.realtime.RealtimePriorityDispatcher;
import ovm.services.threads.PriorityOVMDispatcher;
import ovm.services.threads.PriorityOVMThread;
import ovm.services.threads.TimedSuspensionThreadManager;
import ovm.services.threads.UserLevelThreadManager;
import ovm.util.OVMError;
import s3.services.realtime.PriorityInheritanceOVMThreadImpl;

/**
 * This {@link OVMThread} provides a thread API similar to that of
 * {@link java.lang.Thread} (hence the name) but restricted to those
 * things that make sense as an OVM thread when there is no Java runtime
 * personality available. 
 * <p>We extend {@link PriorityInheritanceOVMThreadImpl} because when
 * priority inheritance is used all threads must support it. Ideally we
 * would introduce that parent class through an aspect depending on the
 * current configuration.
 *
 * <p>This class is quite complicated because unlike {@link java.lang.Thread}
 * there isn't a dedicated dispatcher to make things simple to express in the
 * thread class. This class combines elements of {@link java.lang.Thread},
 * {@link s3.services.java.ulv1.JavaOVMThreadImpl} and
 * {@link s3.services.java.ulv1.JavaDispatcherImpl}. Some of this may be
 * overkill.
 * <p><b>Note that this class is inherently for user-level threading</b>.
 * <h3>Usage Notes</h3>
 * <p>Unlike {@link java.lang.Thread} the {@link #setPriority}
 * method does not change the actual runtime behaviour of the thread. Priority
 * changes should be done via the dispatcher, which in turn will use the
 * threads priority methods to store the priority value. For convenience,
 * the static {@link #setPriority(JLThread, int)} method can be used instead
 * of having to deal with the dispatcher directly. If we don't have a
 * priority dispatcher configured then this will throw an exception. You can
 * query if this is the case using {@link #canSetPriority}.
 *
 * <p>A newly created thread inherits the priority of the thread that creates 
 * it. If priorities are not available in the current configuration then the 
 * value is implementation specific (probably zero for a default int value).
 *
 * <p>As with {@link java.lang.Thread} this class supports a 
 * {@link #sleep(long) sleep} method, however sleeping requires that
 * time-based suspension is supported in the current configuration. To allow
 * for the use of this class in both circumstances the sleep method will
 * throw an exception if sleeping is not supported. Whether or not sleeping is
 * supported can be queried using {@link #canSleep}.
 * <p>Also remember that at present there are no monitor methods available in
 * OVM, so we can't implement <code>join</code> either.
 *
 * @author David Holmes
 */
public class JLThread extends PriorityInheritanceOVMThreadImpl
    implements Runnable 
{

    /** don't generate InterruptedException in sleep() **/
    static final boolean NO_THROW = false; //true;
    
    // we have access to the current dispatcher from our superclass,
    // but we don't know whether it is a priority dispatcher or not.

    /** The currently configured timed-suspension thread manager, if any */
    protected static final TimedSuspensionThreadManager sleepMan;

    /** The currently configured user-level thread manager */
    protected static final UserLevelThreadManager tm;

    // store locally for quicker access
    public static final int MIN_PRIORITY;
    public static final int NORM_PRIORITY;
    public static final int MAX_PRIORITY;


    // This static initializer runs at image build time and expects to set
    // all the above static references from the current configuration.
    // Ideally this class would never get loaded except in a configuration
    // in which all the initialization is guaranteed to succeed, but that
    // is not yet the case. So this is written such that finding null values
    // or the wrong types of service instances, is not considered an error.
    // - DH 1 March 2005

    static {
        ThreadManager t = 
            ((ThreadServicesFactory)ThreadServiceConfigurator.config.
             getServiceFactory(ThreadServicesFactory.name)).getThreadManager();

        if (t instanceof TimedSuspensionThreadManager) {
            sleepMan = (TimedSuspensionThreadManager)t;
        }
        else
            sleepMan = null;

        if (t instanceof UserLevelThreadManager) {
            tm = (UserLevelThreadManager)t;
        }
        else
            tm = null;

        // NOTE: if we stop extending RealtimeOVMThreadImpl then change these
        // to the non-RT values
        if (dispatcher instanceof RealtimePriorityDispatcher) {
//             d("JLThread updating constants to work with real-time priority dispatcher");
            RealtimePriorityDispatcher d = (RealtimePriorityDispatcher) dispatcher;
            MIN_PRIORITY = d.getMinRTPriority();
            MAX_PRIORITY = d.getMaxRTPriority();
            // ensure no overflow
            NORM_PRIORITY = MIN_PRIORITY/2 + MAX_PRIORITY/2;
        }
        else if (dispatcher instanceof PriorityOVMDispatcher) {
//             d("JLThread updating constants to work with priority dispatcher");
            PriorityOVMDispatcher d = (PriorityOVMDispatcher) dispatcher;
            MIN_PRIORITY = d.getMinPriority();
            MAX_PRIORITY = d.getMaxPriority();
            // ensure no overflow
            NORM_PRIORITY = MIN_PRIORITY/2 + MAX_PRIORITY/2;
        }
        else {
            MIN_PRIORITY = NORM_PRIORITY = MAX_PRIORITY = 0;
        }
    }

    public static void printSysInfo(OVMThread t) {
        boolean enabled = tm.setReschedulingEnabled(false);
        boolean isSleeping = false, isReady = false;
        try {
            isSleeping = sleepMan.isSleeping(t);
            isReady = tm.isReady(t);
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }
        BasicIO.out.println(
            "\nRescheduling is " + ( enabled ? "enabled" : "disabled") +
            "\nReady queue length = " + tm.getReadyLength() +
            "\n" + t + " is " + (isReady ? "ready" : "not ready") +
            "\n" + t + " is " + (isSleeping ? "sleeping" : "not sleeping") 
            );
    }

    /** For autonumbering anonymous threads */
    private static int threadNumber;

    /** Lock object used in absence of support for static synchronized 
     * methods
     */
    private static Object staticLock = new Object();


    // These are the constants dealing with the thread lifecycle

    /** An unstarted, unstopped Thread */
    protected static final int PRISTINE_THREAD = 0x0;

    /** A thread upon which start() has been invoked
     */
    protected static final int STARTED_THREAD = 0x1;

    /** A terminated thread */
    protected static final int TERMINATED_THREAD = 0x2;

    // these constants deal with the threads execution state

    /**
     * Thread has not yet been started by the dispatcher 
     */
    protected static final int NOT_STARTED = 0x1000;

    /**
     * Thread is currently ready to execute. It may actually be executing but
     * we can't tell that.
     */
    protected static final int READY = 0x0000;

    /**
     * Thread has terminated.
     */
    protected static final int TERMINATED = 0x2000;

    /**
     * Mask to see if thread is in any blocked state
     */
    protected static final int BLOCKED_MASK = 0x1000;

    /**
     * Thread is currently blocked entering a monitor
     * Not supported yet.
     */
    protected static final int BLOCKED_MONITOR = BLOCKED_MASK | 0x0001;

    /**
     * Thread is currently blocked doing a monitor wait
     * Not supported yet.
     */
    protected static final int BLOCKED_WAIT = BLOCKED_MASK | 0x0002;

    /**
     * Thread is currently blocked doing a timed monitor wait
     * Not supported yet.
     */
    protected static final int BLOCKED_TIMEDWAIT = BLOCKED_MASK | 0x0004;

    /**
     * Thread is currently blocked doing a sleep
     */
    protected static final int BLOCKED_SLEEP = BLOCKED_MASK | 0x0008;

    /**
     * Thread is currently blocked doing an uninterruptible sleep
     */
    protected static final int BLOCKED_SLEEP_NOINTERRUPT = BLOCKED_MASK | 0x0010;

    /**
     * Thread is currently blocked on an I/O call
     */
    protected static final int BLOCKED_IO = BLOCKED_MASK | 0x0020;

    // The instance variables associated with each thread

    /** The name of this thread */
    private volatile String name;


    /** The associated Runnable object, if any */
    protected /* final */ Runnable target;

    /**
     * A <code>JLThread</code> has two states associated with it:
     * <ul>
     * <li>its execution state
     * <li>its lifecycle state
     * </ul>
     * <p>This is its lifecycle state, which can be one of either
     * {@link #PRISTINE_THREAD}, {@link #STARTED_THREAD} or
     * {@link #TERMINATED_THREAD}. This field is always set while holding
     * the internal {@link #lock} of this thread.
     *
     * @see #executionState
     */
    protected volatile int lifecycleState = PRISTINE_THREAD;

    /**
     * A <code>JLThread</code> has two states associated with it:
     * <ul>
     * <li>its execution state
     * <li>its lifecycle state
     * </ul>
     * <p>This is its execution state (ready, blocked etc). This field is only
     * set when the thread manager has rescheduling disabled.
     *
     * @see #lifecycleState
     */
    protected volatile int executionState = NOT_STARTED;

    /**
     * Query if the current thread is in a blocked state
     * @return <code>true</code> if the current execution state is one of the
     * the blocking states, and <code>false</code> otherwise.
     */
    protected final boolean isBlocked() {
        return (executionState & BLOCKED_MASK) != 0;
    }

    /**
     * Query the current execution state of this thread.
     *
     * @return the current execution state
     */
    protected final int getExecutionState() {
        return executionState;
    }



    /**
     * Mark this thread as blocked on I/O depending on the argument. 
     * This is for use by an I/O Manager.
     * <p><b>Note:</b> Only a thread that is ready can be marked as blocked.
     * @param blocked if <tt>true</tt> then the current thread is marked
     * as being in the {@link #BLOCKED_IO} state; otherwise it is marked
     * as being in the {@link #READY} state.
     *
     * <p><b>Note:</b> Interruption of I/O is not supported. But interrupt
     * checks can be made before the I/O commences.
     *
     * <p>This method should only be invoked from an atomic region.
     * (ie. rescheduling disabled).
     *
     * @throws OVMError.IllegalState if the thread was not ready when marked as
     * blocked
     * @throws OVMError.IllegalState if the thread was not blocked when marked
     * as blocked
     */
    public void setBlockedIO(boolean blocked) {
        if (blocked) {
            if (executionState != READY) {
                throw new OVMError.IllegalState("thread not ready when blocked");
            }
            executionState = BLOCKED_IO;
        }
        else {
            if (executionState != BLOCKED_IO) {
                throw new OVMError.IllegalState("thread not blocked when unblocked");
            }
            executionState = READY;

        }
    }


    /** The object used for locking and synchronization.
    */
    protected final  Object lock = new Object();

    /** the interrupt state of this thread */
    protected boolean interrupted = false;

    // static methods

    /** For autonumbering anonymous threads */
    private static int nextThreadNum() {
        synchronized(staticLock) {
            return threadNumber++;
        }
    }

    /**
     * Query if the {@link #sleep} method is currently enabled for threads.
     * @return <code>true</code> if it is and <code>false</code> otherwise.
     */
    public static boolean canSleep() {
        return (sleepMan != null);
    }


    /**
     * Query if the {@link #setPriority(JLThread, int)} method is currently 
     * enabled for threads.
     * @return <code>true</code> if it is and <code>false</code> otherwise.
     */
    public static boolean canSetPriority() {
        return (dispatcher instanceof PriorityOVMDispatcher);
    }
    
    /**
     * Sets the priority of the given thread such that the priority change
     * is reflected in the runtime behaviour of the thread.
     * @param thread the thread whose priority is to be set
     * @param priority the new priority value
     * @throws OVMError.Configuration if the system is not configured
     * for priority dispatching.
     * @throws IllegalArgumentException if the new priority value is out of
     * range for the type of thread and current dispatcher
     *
     * @see #canSetPriority
     */
    public static void setPriority(JLThread thread, int priority) {
        if (canSetPriority()) {
            ((PriorityOVMDispatcher)dispatcher).setPriority(thread, priority);
        }
        else {
            throw new OVMError.Configuration("priority dispatcher not configured");
        }
    }

    /**
     * Return the maximum supported priority value.
     * @return the maximum supported priority value, or zero if priority is
     * not supported in the current configuration.
     */
    public static int getMaxPriority() {
        return MAX_PRIORITY;
    }

    /**
     * Return the minimum supported priority value.
     * @return the minimum supported priority value, or zero if priority is
     * not supported in the current configuration.
     */
    public static int getMinPriority() {
        return MIN_PRIORITY;
    }

    /**
     * Return the default or "normal" priority value.
     * @return the default or "normal" priority value, or zero if priority is
     * not supported in the current configuration.
     */
    public static int getNormPriority() {
        return NORM_PRIORITY;
    }

    /**
     * Returns a reference to the currently executing thread object.
     * @return  the currently executing thread.
     * @throws ClassCastException if the current thread is not a 
     * <code>JLThread</code>
     */
    public static JLThread currentThread() {
        return (JLThread)dispatcher.getCurrentThread();
    }

    /**
     * Causes the currently executing thread object to temporarily pause 
     * and allow other threads to execute. 
     */
    public static void yield() {
        dispatcher.yieldCurrentThread();
    }

    /**	
     * Causes the currently executing thread to sleep (temporarily cease 
     * execution) for the specified number of milliseconds. 
     *
     * @param      millis   the length of time to sleep in milliseconds.
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    public static void sleep(long millis) throws InterruptedException {
        sleep(millis, 0);
    }

    /**
     * Causes the currently executing thread to sleep (cease execution) 
     * for the specified number of milliseconds plus the specified number 
     * of nanoseconds. 
     *
     * @param      millis   the length of time to sleep in milliseconds.
     * @param      nanos    0-999999 additional nanoseconds to sleep.
     * @exception  IllegalArgumentException  if the value of millis is 
     *             negative or the value of nanos is not in the range 
     *             0-999999.
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    public static void sleep(long millis, int nanos) throws InterruptedException {
        sleepInternal(millis, nanos, true);
    }

    /**
     * Causes the currently executing thread to sleep (cease execution) 
     * for the specified number of milliseconds plus the specified number 
     * of nanoseconds. This is the same as {@link #sleep} except that the
     * thread will not wakeup if an interrupt occurs.
     *
     * @param      millis   the length of time to sleep in milliseconds.
     * @param      nanos    0-999999 additional nanoseconds to sleep.
     * @exception  IllegalArgumentException  if the value of millis is 
     *             negative or the value of nanos is not in the range 
     *             0-999999.
     */
    public static void sleepUninterruptible(long millis, int nanos) {
        try {
            sleepInternal(millis, nanos, false);
        }
        catch(InterruptedException ex) {
            throw new OVMError.Internal("uninterruptible sleep interrupted: " + ex.getMessage());
        }
    }

    /**
     * The internal mechanics for doing relative sleeps
     * <p>Should just forward to sleepAbsoluteInternal - DH
     * @param millis   the length of time to sleep in milliseconds.
     * @param nanos    0-999999 additional nanoseconds to sleep.
     * @param interruptable if <code>true</code> then the thread will wake
     * up if it is interrupted.
     * @throws InterruptedException if the sleep was interrupted and the
     * <code>interruptable</code> parameter was set <code>true</code>
     */
    private static void sleepInternal(long millis, int nanos, 
                                      boolean interruptable) 
        throws InterruptedException 
        {
	if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
	}
	if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
				"nanosecond timeout value out of range");
	}
        if (canSleep()) {
            JLThread current = JLThread.currentThread();
            // the check for interruption must be atomic with respect to
            // doing the sleep. So we must disable rescheduling
            boolean enabled = tm.setReschedulingEnabled(false);
            try {
                if (interruptable) {
                    if (JLThread.interrupted()) {
			if (NO_THROW) {
			    BasicIO.out.println("sleep(): ignoring interrupt");
			    return;
			}
                        throw new InterruptedException("interruption on entry to sleep");
                    }
                    current.executionState = BLOCKED_SLEEP;
                }
                else {
                    current.executionState = BLOCKED_SLEEP_NOINTERRUPT;
                }

                long fullNanos = nanos +
                    millis * ovm.core.services.timer.TimeConversion.NANOS_PER_MILLI;
                if (fullNanos != 0) { // don't sleep at all for zero
                    sleepMan.sleep(current, fullNanos);
                }
                current.executionState = READY;
            }
            finally {
                tm.setReschedulingEnabled(enabled);
            }
            // a negative delta indicates an interrupted sleep. Otherwise
            // the delta is always positive.
            if (current.getDelta() < 0) {
                // must have been interrupted
                assert current.isInterrupted() : "not interrupted when it should have been";
                JLThread.interrupted(); // clear
		if (NO_THROW) {
		    BasicIO.out.println("sleep(): ignoring interrupt");
		    return;
		}
                throw new InterruptedException("sleep interrupted");
            }
        }
        else {
            throw new OVMError.Configuration("timed suspension thread manager needed for sleep");
        }
    }

    /**
     * Causes the currently executing thread to sleep (cease execution) 
     * until the specified absolute point in time.
     *
     * @param wakeupTime the absolute time, measured in nanoseconds since
     * the EPOCH, when the current thread should wakeup from its sleep.
     * @return <code>true</code>if the thread actually sleeps, and 
     * <code>false</code> if the wakeup time has already passed.
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    public static boolean sleepAbsolute(long wakeupTime) throws InterruptedException {
        return sleepAbsoluteInternal(wakeupTime, true);
    }

    /**
     * Causes the currently executing thread to sleep (cease execution) 
     * until the specified absolute point in time. Unlike
     * {@link #sleepAbsolute} the thread is not woken up if an interrupt
     * occurs.
     *
     * @param wakeupTime the absolute time, measured in nanoseconds since
     * the EPOCH, when the current thread should wakeup from its sleep.
     * @return <code>true</code>if the thread actually sleeps, and 
     * <code>false</code> if the wakeup time has already passed.
     */
    public static boolean sleepAbsoluteUninterruptible(long wakeupTime){
        try {
            return sleepAbsoluteInternal(wakeupTime, false);
        }
        catch(InterruptedException ex) {
            throw new OVMError.Internal("uninterruptible sleepAbsolute interrupted: " + ex.getMessage());
        }
    }

    /**
     * The internal mechanics for doing absolute sleeps
     *
     * @param wakeupTime the absolute time at which this sleep should finish
     * @param canInterrupt if <code>true</code> then the thread will wake
     * up if it is interrupted.
     * @throws InterruptedException if the sleep was interrupted and the
     * <code>canInterrupt</code> parameter was set <code>true</code>
     */
    private static boolean sleepAbsoluteInternal(long wakeupTime, 
                                          boolean canInterrupt) 
        throws InterruptedException 
        {
        if (canSleep()) {
            JLThread current = JLThread.currentThread();
            boolean retval = true;

            // the check for interruption must be atomic with respect to
            // doing the sleep. So we must disable rescheduling

            boolean enabled = tm.setReschedulingEnabled(false);
            try {
                if (canInterrupt) {
                    if (JLThread.interrupted()) {
			if (NO_THROW) {
			    BasicIO.out.println("sleep(): ignoring interrupt");
			    return false;
			}
                        throw new InterruptedException("interruption on entry to sleep");
                    }
                    current.executionState = BLOCKED_SLEEP;
                }
                else {
                    current.executionState = BLOCKED_SLEEP_NOINTERRUPT;
                }
                retval = sleepMan.sleepAbsolute(current, wakeupTime);
                current.executionState = READY;
            }
            finally {
                tm.setReschedulingEnabled(enabled);
            }
            // should never happen for non-interruptible, so leave the test
            // as an extra sanity check.
            if (current.getDelta() < 0) {
                // must have been interrupted
                assert current.isInterrupted() : "not interrupted when it should have been";
                JLThread.interrupted(); // clear
		if (NO_THROW) {
		    BasicIO.out.println("sleep(): ignoring interrupt");
		    return true;
		}
                throw new InterruptedException("sleep interrupted");
            }
            return retval;
        }
        else {
            throw new OVMError.Configuration("timed suspension thread manager needed for sleep");
        }
    }


    // constructors
    
    /**
     * Create a thread with no associated {@link Runnable} and a name of the
     * form <code>Thread-n</code> where <code>n</code> is the next available
     * thread number. 
     * The initial priority of the thread is that of the creating thread.
     */
    public JLThread() {
	this((Runnable)null, "Thread-" + nextThreadNum());
    }

    /**
     * Create a thread with the associated {@link Runnable} and a name of the
     * form <code>Thread-n</code> where <code>n</code> is the next available
     * thread number.
     * The initial priority of the thread is that of the creating thread.
     * @param target the Runnable whose run method should be invoked
     */
    public JLThread(Runnable target) {
	this(target, "JLThread-" + nextThreadNum());
    }

    /**
     * Creates a thread with no associated {@link Runnable} and the given name.
     * The initial priority of the thread is that of the creating thread.
     * @param name the name to give the thread
     * @throws IllegalArgumentException if name is null.
     */
    public JLThread(String name) {
	this((Runnable)null, name);
    }

    /**
     * Create a thread with the associated {@link Runnable} and the given name.
     * The initial priority of the thread is that of the creating thread, or
     * {@link #NORM_PRIORITY} if the current thread is not a priority thread.
     * @param name the name to give the thread
     * @param target the Runnable whose run method should be invoked
     * @throws IllegalArgumentException if name is null.
     */
    public JLThread(Runnable target, String name) {
	this.name = name;
        this.target = target;
        OVMThread current = dispatcher.getCurrentThread();
        if (current instanceof PriorityOVMThread) {
            this.setPriority(((PriorityOVMThread)current).getPriority());
        }
        else {
            this.setPriority(NORM_PRIORITY);
        }
        setBasePriority(getPriority());
//        BasicIO.out.println("Created JLThread " + name + " with priority " + getPriority() + " and base priority " + getBasePriority() );
    }

    public JLThread(OVMThreadContext ctx, String name) {
        super(ctx);
	this.name = name;
        this.target = null;
        OVMThread current = dispatcher.getCurrentThread();
        if (current instanceof PriorityOVMThread) {
            this.setPriority(((PriorityOVMThread)current).getPriority());
        }
        else {
            this.setPriority(NORM_PRIORITY);
        }
        setBasePriority(getPriority());
//        BasicIO.out.println("Created JLThread " + name + " with priority " + getPriority() + " and base priority " + getBasePriority() );
    }        

    // Actual instance methods

    /**
     * A simple helper method for executing run() in the right context. For a
     * normal thread there is nothing special to do, while a RT thread will
     * override to maintain the scope stack, for instance
     *
     */
    protected void executeRun() {
            assert executionState == READY : " JLThread in wrong execution state - make sure you used t.start() not dispatcher.startThread(t)";
        run();
    }

    /** The method that is initially executed when a Thread is started.
        This method takes care of exception handling in the thread and
        proper termination of the thread. Note that we have to be very careful
        about getting exceptions from library code as we could be terminating
        due to an uncaught exception already.
    */
    protected final void doRun() {
        try {
            executeRun();   // easy to change the context in which run is executed
        }
        catch(Throwable t) {
            BasicIO.err.println(t);
            t.printStackTrace();
        }
        finally {

            // allow GC for objects we can't possibly use again.
            target = null;

            synchronized(lock) {
                //lock.notifyAll();
                lifecycleState |= TERMINATED_THREAD; // isAlive will not return false
            }
            // actually terminate the thread
            try {
                boolean enabled = tm.setReschedulingEnabled(false);
                try {
                    executionState = TERMINATED; 
                    d("Thread " + this + " terminating");
                    dispatcher.terminateCurrentThread(); // never returns
                    throw new OVMError.Internal("terminate current thread returned!");
                }
                finally {
                    tm.setReschedulingEnabled(enabled);
                }
            }
            catch (Throwable t) {
                BasicIO.err.print("Error on thread termination - ABORTING: ");
                BasicIO.err.println(t);
                t.printStackTrace();
                ovm.core.execution.Native.exit_process(-1);
            }

        }
    }    




    /**
     * Causes this thread to begin execution; the VM
     * calls the <code>run</code> method of this thread. 
     * <p>
     * The result is that two threads are running concurrently: the 
     * current thread (which returns from the call to the 
     * <code>start</code> method) and the other thread (which executes its 
     * <code>run</code> method). 
     *
     * @exception  IllegalThreadStateException  if the thread was already
     *               started.
     */
    public void start() {
        try {
            boolean doStart = false;
            synchronized(lock) {
                if (lifecycleState == PRISTINE_THREAD) {
                    // set the state to started before doing the real start
                    // as the real start could cause a reschedule.
                    lifecycleState = STARTED_THREAD; 
                    doStart = true;
                }
                else if ( (lifecycleState & STARTED_THREAD) != 0) {
                    throw new IllegalThreadStateException("Thread already started");
                }
                else {
                    // we're not started but not pristine so stop() must have been
                    // invoked and we are still-born - so do nothing
                    // assert: state & STOPPING_THREAD != 0
                }
            }

            if (doStart) {
                // at this point we can't be restarted but isAlive() will 
                // return false.

                // can't hold lock doing this as there could be a reschedule
                boolean enabled = tm.setReschedulingEnabled(false);
                try {
                    executionState = READY; 
                    invokeDispatcherStart();
                }
                finally {
                    tm.setReschedulingEnabled(enabled);
                }
            }
        } // debugging
        catch (Error e) {
            BasicIO.out.println("JLThread.start completed abnormally - lock released");
            throw e;
        }
        catch (RuntimeException r) {
            BasicIO.out.println("JLThread.start completed abnormally - lock released");
            throw r;
        }

    }
    

    /**
     * Invokes the appropriate dispatcher functionality to start the thread.
     * In this case we forward directly to {@link PriorityOVMDispatcher#startThread}.
     * Subclasses may need to override this to change the details of what
     * occurs.
     */
    protected void invokeDispatcherStart() {
       dispatcher.startThread(this); 
    }

    /**
     * If this thread was constructed using a separate 
     * <code>Runnable</code> run object, then that 
     * <code>Runnable</code> object's <code>run</code> method is called; 
     * otherwise, this method does nothing and returns. 
     * <p>
     * Subclasses of <code>JLThread</code> should override this method. 
     *
     */
    public void run() {
	if (target != null) {
	    target.run();
	}
    }


    /**
     * Interrupts this thread. Interruption of blocking I/O is not suported.
     */
    public void interrupt() {
        /* Note that setting the interrupt state and causing the thread to wakeup must be
           done atomically. Otherwise between setting the state and doing the wakeup, the target
           thread might poll for interrupt, clear it and then do a wait/sleep which would then
           be incorrectly interrupted.
        */
	synchronized(lock) { 
            interrupted = true;
            if (isAlive()) {
                doInterrupt(this);
            }
        }
    }

    /** The mechanics of interruption
     */
    static void doInterrupt(JLThread thread) {
        // NOTE: no other thread can change the targets state as we hold
        // the lock. But the passage of time could change it's state, either
        // due to the expiry of a timed-wait or sleep, or the completion of I/O
        boolean enabled = tm.setReschedulingEnabled(false);
        try {
            switch(thread.executionState) {
                case NOT_STARTED:
                case READY:
                case BLOCKED_MONITOR: // can't tell this but its ok
                case BLOCKED_SLEEP_NOINTERRUPT:
                case BLOCKED_IO:
                    break; // nothing to do

                case BLOCKED_SLEEP:
                    if (sleepMan.wakeUp(thread)) {
                        thread.executionState = READY;
                    }
                    else {
                        // the thread manager doesn't know about thread
                        // state, so the thread won't return to the
                        // ready state until it gets to run again.
                        // Hence the sleeping thread is actually in the
                        // ready queue. So we need do nothing.
                        assert tm.isReady(thread):
			    "thread not ready or sleeping";
                    }
                    break;
                default: 
                    throw new OVMError.Internal(
                        "Unknown thread execution state: " + 
                        thread.executionState);
            }
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }

    }
    /**
     * Tests whether the current thread has been interrupted.  The
     * <i>interrupted status</i> of the thread is cleared by this method.  In
     * other words, if this method were to be called twice in succession, the
     * second call would return false (unless the current thread were
     * interrupted again, after the first call had cleared its interrupted
     * status and before the second call had examined it).
     *
     * @return  <code>true</code> if the current thread has been interrupted;
     *          <code>false</code> otherwise.
     * @see #isInterrupted()
     */
    public static boolean interrupted() {
	return currentThread().isInterrupted(true);
    }

    /**
     * Tests whether this thread has been interrupted.  The <i>interrupted
     * status</i> of the thread is unaffected by this method.
     *
     * @return  <code>true</code> if this thread has been interrupted;
     *          <code>false</code> otherwise.
     * @see     #interrupted()
     */
    public boolean isInterrupted() {
	return isInterrupted(false);
    }

    /**
     * Tests if some Thread has been interrupted.  The interrupted state
     * is reset or not based on the value of the argument clear
     */
    private boolean isInterrupted(boolean clear) {
        synchronized(lock) {
            boolean _interrupted = this.interrupted;
            if (clear)
                this.interrupted = false;
            return _interrupted;
        }
    }


    /**
     * Tests if this thread is alive. A thread is alive if it has 
     * been started and has not yet terminated. 
     *
     * @return  <code>true</code> if this thread is alive;
     *          <code>false</code> otherwise.
     */
    public final boolean isAlive() {
        // we make this atomic wrt. the thread manager
        boolean enabled = tm.setReschedulingEnabled(false);
        try {
            return (executionState != NOT_STARTED) &&
                (executionState != TERMINATED);
        }
        finally {
            tm.setReschedulingEnabled(enabled);
        }
    }


    /**
     * Changes the name of this thread to be equal to the argument 
     * <code>name</code>. 
     *
     * @param      name   the new name for this thread.
     */
    public final void setName(String name) {
	this.name = name;
    }

    /**
     * Returns this thread's name.
     *
     * @return  this thread's name.
     * @see     #setName
     */
    public final String getName() {
	return name;
    }


    /**
     * Prints a stack trace of the current thread. This method is used 
     * only for debugging. 
     *
     */
    public static void dumpStack() {
	new OVMError("Stack trace").printStackTrace();
    }


    /**
     * Returns a string representation of this thread, including the 
     * thread's name, priority, and thread group (if the Thread is alive)
     *
     * @return  a string representation of this thread.
     */
    public String toString() {
        // we throw in some extra detail for debugging assistance
        return "Thread[" + name + ", " + getPriority() + ", " + 
            (isAlive() ? "alive" : ((lifecycleState & STARTED_THREAD) == 0 ? "not-started" : "terminated")) + "]";
    }


    /** The poll period for join(). */
    static final long JOIN_POLL_TIME = 500; // milliseconds

    /**
     * Blocks the current thread until this thread has terminated, or the
     * current thread is interrupted.
     * <p>This method uses a sleep based polling approach as there is no
     * monitor mechanism for waiting in the kernel. 
     * <p><b>Only use this method
     * when sleeping is enabled in the configuration.</b>
     *
     * @throws InterruptedException if the current thread is interrupted.
     */
    public void join() throws InterruptedException {
        while (isAlive()) {
            sleep(JOIN_POLL_TIME, 0);
        }
    }

}



