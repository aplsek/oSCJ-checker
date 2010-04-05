/*
 * TimedSuspensionUserLevelThreadManagerImpl.java
 *
 */
package s3.services.threads;

import ovm.core.execution.Native;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.timer.TimerManager;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.TimerServicesFactory;
import ovm.services.threads.TimedSuspensionThreadManager;
import ovm.util.OVMError;
import s3.util.PragmaNoPollcheck;
import s3.util.Visitor;
import s3.util.queues.SingleLinkDeltaElement;
/**
 * This thread manager extends the {@link BasicUserLevelThreadManagerImpl} by
 * adding support for timed-suspension (ie. sleep), which also introduces
 * the need to disable rescheduling during critical sections, if the
 * general event manager is not being used. 
 * The basic operation of the thread manager is unchanged.
 * <p>Timed-suspension is provided through the use of two &quot;sleep&quot;
 * queues: one for relative sleeps and one for absolutes; and a thread may 
 * only appear in that queue once.
 * <h3>Synchronization Control</h3>
 * <p>Although this is a user-level threading implementation preemption can
 * occur at anytime due to the waking up of a thread as time elapses, or
 * the completion of I/O. This means that interference is possible and so 
 * some form of synchronization must be used. Interference is avoided by 
 * invoking <code>reschedulingEnabled(false)</code> before entering critical 
 * sections and then restoring the rescheduling state after the critical 
 * section. 
 * <b>It is the responsibility of the calling code to protect access to the
 * thread manager</b>.
 * <p>The timer interrupt will lead to the invocation of updater.fire if 
 * there is no event manager. Context switching happens automatically when
 * needed once rescheduling is enabled.
 * <p>Note: No I/O interrupts are possible without the event manager.
 * <p>Rescheduling should be disabled at the lowest level of all the 
 * interrupt/polling routines: either the timer standalone or the event
 * manager.  We do it at the CSA level when performing the
 * interrupt upcalls. Though we discovered the hard way that event processing
 * has to be disabled at the native level whilst processing previous events.
 * <p>We disable pollchecks on all methods that are invoked with rescheduling
 * disabled, or which are trivial in-lineable methods - as an optimisation.
 *
 * <h3>Notes</h3>
 * <p>The sleep function is written such that any thread can be put to sleep.
 * The rationale behind this was to allow a single delta queue to deal with
 * all timed-waits of any kind - you would customise the visitor to enquire
 * why a particular thread was in the delta queue. Allowing any thread to be
 * put to sleep also supports the delayed startup semantics of real-time
 * periodic threads - we can put them in the sleep queue instead of the run
 * queue.
 *
 * @author David Holmes
 *
 */
public class TimedSuspensionUserLevelThreadManagerImpl 
    extends BasicUserLevelThreadManagerImpl
    implements TimedSuspensionThreadManager {

    
    /** The resolution of the sleep queue. That is, the number of
     *  nanoseconds between sleep queue updates.
     */
    protected long sleepMin;
    
    /** Reference to the timer manager being used */
    protected TimerManager timer;
    
    public static final boolean DEBUG = false;
    
    /** 
     * The visitor class used to deal with waking threads.
     * This visitor simply makes each of the awoken threads ready to run.
     * 
     */
    protected class ThreadWaker implements Visitor {
        // Note: rescheduling is disabled when this is called
        public void visit(Object thread) throws PragmaNoPollcheck {
            if (DEBUG) Native.print_string("About to makeReady " + thread + "\n");
            makeReady((OVMThread)thread);
            if (DEBUG) Native.print_string("Returned from makeReady\n");
        }
    }
    
    /**
     * Factory for getting the visitor instance
     */
    protected Visitor getVisitor() {
        return new ThreadWaker();
    }
    
    /** The actional visitor used to deal with waking threads */
    protected final Visitor waker = getVisitor();
    


    /* Implementation of ThreadManager methods */

    public void init() {
        super.init();
        timer = ((TimerServicesFactory)ThreadServiceConfigurator.config.getServiceFactory(TimerServicesFactory.name)).getTimerManager();
        if (timer == null) {
            throw new OVMError.Configuration("need a configured timer service");
        }
        isInited = true;
    }


    public void start() {
        // must start the timer before we check the period actually used
        if (!timer.isStarted())
            timer.start();
        sleepMin = timer.getTimerInterruptPeriod();
        //BasicIO.out.println(sleepMin);
        super.start();
    }

    /* implementations of methods of TimedSuspensionThreadManager */


    /**
     * @param t the thread to put to sleep. This thread must
     * be an instance of {@link TimedSuspensionOVMThreadImpl}.
     * @throws OVMError.IllegalState if this thread is already in the sleep
     * queue, or is already linked into a delta queue.
     * @throws OVMError.IllegalArgument if the sleep time is <= 0
     */
    public void sleep(OVMThread t, long nanos) {
        if (nanos <= 0) {
            throw new OVMError.IllegalArgument("sleep time must be > 0");
        }

        TimedSuspensionOVMThreadImpl thread = (TimedSuspensionOVMThreadImpl) t;
	assert schedulingEnabled == false:  "scheduling still enabled";
	assert timer.isRunning(): "timer manager is not running";
	assert thread.getNextDelta() == null:
	    "thread already linked into delta queue";

        // need to adjust nanos to ensure we sleep >= nanos regardless of
        // the actual clock update granularity. This means we must round up
        // to a multiple of sleepMin and add sleepMin again to ensure
        // we wait at least two clock ticks (the first of which could happen
        // just after we go in the queue, thus reducing nanos by sleepMin
        // straight away)
        // NOTE: if events have been disabled for a while and we haven't hit
        // a pollcheck since they were enabled, then there could be multiple
        // outstanding ticks which still cause this sleep to return 
        // prematurely. DH 25/8/2004
	// NOTE re above: this is now mostly fixed; the timer manager will
	// add the number of outstanding ticks to the relative sleep time.
	// FP 22/2/2006

	if (false) {
	    Native.print_string("sleep: asked to sleep for ");
	    Native.print_long(nanos);
	}

        long rem = nanos%sleepMin;
        if (rem > 0)
            nanos = nanos - rem  + 2*sleepMin;
        else
            nanos = nanos + sleepMin;
	
	if (nanos < 0) { // overflow - a very long sleep
	    nanos = Long.MAX_VALUE;
	}
	
	if (false) {
	    Native.print_string(" but will sleep for ");
	    Native.print_long(nanos);
	    Native.print_string("\n");
	}
	    
        thread.setVisitor(waker);
        // current thread will be in the ready queue. Other threads may not.
        if (removeReady(thread)) {
            //                BasicIO.out.println("sleep: inserting former ready thread");
            timer.delayRelative(thread, nanos);
        }
        else {
            //                BasicIO.out.println("sleep: inserting non-ready thread");
            timer.delayRelative(thread, nanos);
        }
        
        if (thread == getCurrentThread()) {
            //                BasicIO.out.println("sleep: current thread inserted");
            runNextThread(); // must switch to runnable thread
        }
    }
            
    /**
     * @param t the thread to put to sleep. This thread must
     * be an instance of {@link TimedSuspensionOVMThreadImpl}
     * @throws OVMError.IllegalState if this thread is already in the sleep
     * queue, or is already linked into a delta queue.
     */
    public boolean sleepAbsolute(OVMThread t, long nanos) {
        TimedSuspensionOVMThreadImpl thread = (TimedSuspensionOVMThreadImpl) t;
	assert schedulingEnabled == false: "scheduling still enabled";
	assert timer.isRunning(): "timer manager is not running";
	assert ((SingleLinkDeltaElement)thread).getNextDelta() == null:
	    "thread already linked into delta queue";

        thread.setVisitor(waker);

        // current thread will be in the ready queue. Other threads may not.
        if (removeReady(thread)) {
            if (!timer.delayAbsolute(thread, nanos)) {
                // no need to sleep so put back in ready queue
                makeReady(thread);
                return false;
            }
        }
        else {
            if (!timer.delayAbsolute(thread, nanos)) {
//                BasicIO.out.println("sleepAbsolute: insert indicates wake time has passed");
                return false;
            }
            else {
//                BasicIO.out.println("sleepAbsolute: thread was inserted");
            }
        }

        if (thread == getCurrentThread()) {
            runNextThread(); // must switch to runnable thread
        }
        return true;
    }

    /*
     * {@inheritDoc}
     * <p>A woken thread is guaranteed to have a negative delta value. This
     * can be used to easily test if the thread was woken early.
     */
    public boolean wakeUp(OVMThread t) throws PragmaNoPollcheck {
        TimedSuspensionOVMThreadImpl thread = (TimedSuspensionOVMThreadImpl) t;
	assert schedulingEnabled == false: "scheduling still enabled";
        if (timer.wakeUpAbsolute(thread) ||
            timer.wakeUpRelative(thread)) {
            thread.setDelta(-1); // set negative as flag
            makeReady(thread);
            return true;
        }
        else {
            return false;
        }
    }

    public boolean isSleeping(OVMThread t) throws PragmaNoPollcheck {
        TimedSuspensionOVMThreadImpl thread = (TimedSuspensionOVMThreadImpl) t;
	assert schedulingEnabled == false: "scheduling still enabled";
        return timer.isDelayedAbsolute(thread) || 
            timer.isDelayedRelative(thread);
    }

    public long getSleepResolution() {
        return sleepMin;
    }

    /**
     * Queries whether this thread manager supports waking up of sleeping
     * threads, which it does.
     * @return true
     */
    public boolean canWakeUp() {
        return true;
    }

}
        








