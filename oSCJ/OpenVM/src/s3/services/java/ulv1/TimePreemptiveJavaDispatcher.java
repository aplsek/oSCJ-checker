/*
 * $HEADER$
 */
package s3.services.java.ulv1;

import ovm.core.services.timer.TimerInterruptAction;
import ovm.core.services.timer.TimerManager;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.TimerServicesFactory;
import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.OVMThread;
import ovm.services.java.JavaDispatcher;
import ovm.util.OVMError;

import s3.util.PragmaNoPollcheck;
/**
 * An experimental dispatcher implementation that provides support for
 * time-preemptive scheduling. Within a priority level each thread can
 * execute for a set number of timer ticks, known as the quantum. After a
 * thread has used up its quantum it is sent to the back of the set of threads
 * of the same priority in the ready queue.
 * <p>The basic operation is as follows:
 * <ul>
 * <li>On each clock tick our handler runs and decrements the used quantum.
 * If this value reaches zero then the current thread is removed from and
 * replaced into the ready queue. The quantum is not reset here.
 * <li>In our <tt>afterContextSwitch</tt> hook we restore the used quanta to 
 * the allowed maximum.
 * </ul>
 * <p>In this way any time a thread loses the CPU its available quantum gets
 * reset. Arguably, different policies are desirable depending on whether the
 * thread gave up the CPU (by blocking) or was forced to relinquish it (by
 * priority preemption) - however we can't easily make that distinction (only
 * the thread manager can really tell and changing the thread manager behaviour
 * is much harder than augmenting the dispatcher.
 *
 * @author David Holmes
 */
public class TimePreemptiveJavaDispatcher extends JavaDispatcherImpl {

    /**
     * The number of ticks before preemption occurs. The default is 1 to
     * accommodate the usual (non-realtime) 10ms clock tick rate.
     */
    protected int maxTicks = 1;


    /**
     * The number of ticks left to the currently running thread
     */
    protected int ticksLeft;


    /**
     * Our timer manager
     */
    protected TimerManager timer;

    /**
     * Our handler that runs on each clock tick. This will only run when
     * event processing is enabled and runs with rescheduling and event
     * processing disabled - so we know that the current thread is not in a
     * strange state when this executes. However it may be that no thread
     * is ready to run when this executes (the only thread may be sleeping for
     * example) - in this case currentThread still holds a reference to the 
     * last runnable thread, even though it is not in the ready queue. So we
     * have to make sure we don't put it back in prematurely.
     * <p>Even though other events may release
     * threads at the same time (ie within the same event processing action)
     * as this is executed that won't affect the currently recorded
     * <tt>currentThread</tt>. The interaction between this and other event
     * processing depends on which processing occurs first - if other threads
     * of the same priority as the current thread are released in susequent
     * event processing actions then current won't be at the tail, but it will
     * at least be behind any threads of the same priority already in the
     * ready queue.
     */
     protected TimerInterruptAction preempter = new TimerInterruptAction() {
             public void fire(int ticks) throws PragmaNoPollcheck {
                 if (--ticksLeft == 0) {
                     OVMThread current = jtm.getCurrentThread();
                     // only makeReady if it was ready
                     if (jtm.removeReady(current)) {
                         jtm.makeReady(current);
                     }
                     else {
                         // debug placeholder
                     }
                 }
                 else if (ticksLeft < 0){
                     // negative ticks means either the current thread is the
                     // only runnable thread (so no context switch) or the only
                     // context switch was to a new thread (so no afterCTXSW
                     // hook has been executed yet) Resetting ticksLeft has no
                     // effect on the first case and fixes the second.
                     ticksLeft = maxTicks;
                 }
             }
	     public String timerInterruptActionShortName() {
		 return "jdispatcher";
	     }
         };



    /** The singleton instance of this class */
    final static JavaDispatcher instance = new TimePreemptiveJavaDispatcher();

    /**
     * Return the singleton instance of this class 
     * @return the singleton instance of this class 
     */
    public static OVMDispatcher getInstance() {
        return instance;
    }


    /** no construction allowed */
    protected TimePreemptiveJavaDispatcher() {}


    /**
     * On each context switch reset the number of ticks available to the
     * running thread. Note that this gets called on the return from a
     * context switch back to the current thread. The significance of this
     * is that a new thread when run for the first time does not have
     * this hook executed until it is switched away from and then back to 
     * again. The dispatcher doesn't have a hook into the initial running of
     * a thread - though thread's themselves have such hooks, so you could
     * define a special thread subclass to interact with ticksLeft on the
     * initial running of that thread.
     */
    public void afterContextSwitch() throws PragmaNoPollcheck {
        ticksLeft = maxTicks;
    }

    public void init() {
        super.init();
        timer = ((TimerServicesFactory)
                 ThreadServiceConfigurator.config.
                 getServiceFactory(TimerServicesFactory.name)).
            getTimerManager();
        if (timer == null) {
            throw new OVMError.Configuration("need a configured timer service");
        }
        isInited = true;
    }

    public void start() {
        ticksLeft = maxTicks;
        timer.addTimerInterruptAction(preempter);
        super.start();
    }

    public void stop() {
        timer.removeTimerInterruptAction(preempter);
        super.stop();
    }

    /**
     * Set the number of clock ticks to allow a thread to have before it is
     * preempted.
     * @param ticks the number of ticks to allow
     */
    public void setPreemptionTicks(int ticks) {
        if (ticks <=0)
            throw new OVMError.IllegalArgument("ticks must be >= 1");
        maxTicks = ticks;
    }
}
