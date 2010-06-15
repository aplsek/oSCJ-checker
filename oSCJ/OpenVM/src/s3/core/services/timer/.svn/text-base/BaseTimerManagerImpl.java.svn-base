
package s3.core.services.timer;

import ovm.core.execution.NativeInterface;
import ovm.core.services.timer.TimeConversion;
import ovm.core.services.timer.TimerInterruptAction;
import ovm.core.services.timer.TimerManager;
import ovm.core.stitcher.*;
import ovm.core.services.events.EventManager;
import ovm.core.services.events.PollcheckManager;
import ovm.util.OVMError;
import ovm.core.services.process.ForkManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.execution.Native;
import ovm.core.services.memory.*;
import ovm.core.services.io.BasicIO;

import s3.util.queues.*;
import s3.util.PragmaAtomic;
import s3.util.PragmaNoPollcheck;
import s3.services.threads.TimedSuspensionOVMThreadImpl;

public abstract class BaseTimerManagerImpl 
    extends ovm.services.ServiceInstanceImpl
    implements TimerManager, EventManager.EventProcessor {

    static final boolean PRINT_MISSED_AND_NEG = false;

    /**
     * Our list of action objects to fire
     */
    protected TimerInterruptAction[] actions;

    /**
     * The index at which to insert the next action
     */
    protected int index = 0;

    // inherit isInited and isStarted

    /**
     * Are we stopped?
     */
    protected volatile boolean isStopped = false;

    protected long sysPeriod = 10L * TimeConversion.NANOS_PER_MILLI;

    /**
     * the current interrupt period in nanoseconds. Default 10ms.
     */
    protected long period = 10L * TimeConversion.NANOS_PER_MILLI;
    
    protected long multiplier = 1;


    /** The event manager */
    protected EventManager em;

    /**
     * The default size of the actions array. We expect this to be small.
     * We envisage one action for a thread manager, perhaps a second if the
     * thread manager uses time preemption, a third for a single monitor
     * timed-wait queue, and a fourth for the IO manager.
     */
    protected static final int DEFAULT_ACTION_LIST_SIZE = 4;


    /** A timer queue for absolute sleeps/waits */
    protected SingleLinkTimerQueue absQ;

    /** Size of absolute delay queue */
    private int absQSize;
    
    /** A timer queue for relative sleeps/waits */
    protected SingleLinkDeltaQueue relQ;

    /** Size of relative delay queue */
    private int relQSize;


    /**
     * Create a timer manager
     * using the default initial action list size.
     *
     *
     */
    public BaseTimerManagerImpl() {
        this(DEFAULT_ACTION_LIST_SIZE);
    }

    /**
     * Create a timer manager 
     * using the given initial action list size.
     * <p>For a given configuration the number of registered actions may be
     * fixed - in fact in this implementation we expect that - so we don't
     * attempt to do anything fancy in terms of maintaining the action
     * list.
     *
     * @param size The initial size of the action list.
     *
     */
    public BaseTimerManagerImpl(int size) {
        if (size <= 0) {
            size = DEFAULT_ACTION_LIST_SIZE;
        }
        actions = new TimerInterruptAction[size];

        absQ = new SingleLinkTimerQueue();
        relQ = new SingleLinkDeltaQueue();
    }

    // we inherit most docComments

    protected void initHook() {}

    /** Initialisation of the timer manager is trivial, but should be performed
     *  as part of the {@link ovm.services.ServiceInstance service instance}
     *  requirements.
     */
    public void init() {
        em = ((EventServicesFactory) IOServiceConfigurator
              .config
              .getServiceFactory(EventServicesFactory.name))
            .getEventManager();
	
	initHook();

        isInited = true;
    }
    
    /**
     * Last value of interruptCount
     */
    protected volatile int lastCount;

    abstract protected int getCurrentCount();

    // This is the entry point when the event manager is in charge
    public void eventFired() throws PragmaNoPollcheck {

        int currentCount = getCurrentCount(); /* latch value */
        int ticks;

        /* this subtraction yields the correct value even when currentCount
           overflows and becomes negative. Hence there is no overflow problem
           with any of this code.
        */
        ticks = currentCount - lastCount;
        lastCount = currentCount;

	if (PRINT_MISSED_AND_NEG && (ticks > 1 || ticks < 0)) {
	    Native.print_string("Timer manager: eventFired with ticks = ");
	    Native.print_int(ticks);
	    Native.print_string("\n");
	}


	handleTimer(ticks);
    }
    
    protected int outstandingTicks() {
	return getCurrentCount()-lastCount;
    }

    static final boolean DEBUG = false;

    protected void fire(int ticks) {
        if (relQSize > 0) {
            if (DEBUG) Native.print_string("fire: updating relQ\n");
            int i = relQ.update(ticks*period);
            if (i > 0) {
                relQSize -= i;
                if (DEBUG) Native.print_string("fire: update woke on rel queue\n");
            }
            else {
                if (DEBUG) Native.print_string("fire: no wakeup on rel queue\n");
            }
        }

        if (absQSize > 0) {
            if (DEBUG) Native.print_string("fire: updating absQ\n");
            int i = absQ.update();
            if (i > 0) {
                absQSize -= i;
                if (DEBUG) Native.print_string("fire: update woke on abs queue\n");
            }
            else {
                if (DEBUG) Native.print_string("fire: no wakeup on abs queue\n");
            }
        }
    }

    /**
     * @param obj The object to be delayed, which is required to
     * implement the {@link DelayableSingleLinkDeltaElement} interface.
     */
    public void delayRelative(DelayableObject obj, long delay) {
	assert !absQ.contains(((DelayableSingleLinkDeltaElement)obj)):
	    "obj already in abs delay queue";
	assert !relQ.contains(((DelayableSingleLinkDeltaElement)obj)):
	    "obj already in rel rel queue";
	assert delay > 0: "delay <= 0";
        relQ.insert(((DelayableSingleLinkDeltaElement)obj),
		    delay + outstandingTicks()*period);
        relQSize++;
    }


    /**
     * @param obj The object to be delayed, which is required to
     * implement the {@link DelayableSingleLinkDeltaElement} interface.
     */
    public boolean delayAbsolute(DelayableObject obj, long delay) {
	assert !absQ.contains(((DelayableSingleLinkDeltaElement)obj)):
	    "obj already in abs delay queue";
	assert !relQ.contains(((DelayableSingleLinkDeltaElement)obj)):
	    "obj already in rel delay queue";
        boolean ret = absQ.insert(((DelayableSingleLinkDeltaElement)obj), delay);
        if (ret) absQSize++;
        return ret;
    }


    /**
     * @param obj The object to be woken up, which is required to
     * implement the {@link DelayableSingleLinkDeltaElement} interface.
     */
    public boolean wakeUpRelative(DelayableObject obj) 
        throws PragmaNoPollcheck {
        boolean ret = relQ.remove((DelayableSingleLinkDeltaElement)obj);
        if (ret) relQSize--;
        return ret;
    }

    /**
     * @param obj The object to be woken up, which is required to
     * implement the {@link DelayableSingleLinkDeltaElement} interface.
     */
    public boolean wakeUpAbsolute(DelayableObject obj) 
        throws PragmaNoPollcheck {
        boolean ret = absQ.remove((DelayableSingleLinkDeltaElement)obj);
        if (ret) absQSize--;
        return ret;
    }

    /**
     * @param obj The object to be queried, which is required to
     * implement the {@link DelayableSingleLinkDeltaElement} interface.
     */
    public boolean isDelayedRelative(DelayableObject obj) 
        throws PragmaNoPollcheck {
        return relQ.contains((DelayableSingleLinkDeltaElement)obj);
    }


    /**
     * @param obj The object to be queried, which is required to
     * implement the {@link DelayableSingleLinkDeltaElement} interface.
     */
    public boolean isDelayedAbsolute(DelayableObject obj) 
        throws PragmaNoPollcheck {
        return absQ.contains((DelayableSingleLinkDeltaElement)obj);
    }
    
    protected void handleTimer(int ticks) throws PragmaNoPollcheck {
        if (ticks==0) {
            return;
        }

        this.fire(ticks);

        processActions(ticks);
    }


    void processActions(int ticks) {
        for (int i = 0; i < index; i++) {
	    processAction(ticks,i);
        }
    }

    public String eventProcessorShortName() {
	return "timer";
    }

    protected void processAction(int ticks,
				 int i) throws PragmaNoPollcheck {
	try {
	    actions[i].fire(ticks);
	}
	catch (StackOverflowError soe) {
	    // we handle this specially because the other debug code will
	    // likely retrigger a stackoverflow
	    Native.print_string("Warning: timerManager.processActions - stackoverflow has occurred\n");
	    throw soe;
	}
	catch (Throwable t) {
	    // This is debug code but watch for generating secondary
	    // exceptions due to memory problems
	    Object r1 = MemoryPolicy.the().enterExceptionSafeArea();
	    try {
		// Should log somewhere !!!
		Native.print_string("Warning: - Exception in timerInterruptAction: ");
		Native.print_string(t.toString());
		Native.print_string("\n");
	    }
	    catch (Throwable t2) {
		Native.print_string("\nWarning: - secondary exception\n");
	    }
	    finally {
		MemoryPolicy.the().leave(r1);
	    }
	}
    }

    /**
     * Adds the specified {@link TimerInterruptAction} to the list of
     * actions to be fired when the timer interrupt occurs. Actions are
     * fired in the order in which this method is invoked.
     */
    public void addTimerInterruptAction(TimerInterruptAction tia)
	throws PragmaAtomic {
	VM_Area prev=
	    MemoryManager.the()
	    .setCurrentArea(MemoryManager.the().getImmortalArea());
	try {
	    addTimerInterruptActionImpl(tia);
	} finally {
	    MemoryManager.the().setCurrentArea(prev);
	}
    }

    protected void addTimerInterruptActionImpl(TimerInterruptAction tia)
	throws PragmaAtomic {
        if (index > actions.length-1) {
            growList();
        }
        actions[index++] = tia;
    }


    /**
     * Increases the size of the action array when it is full
     * 
     */
    protected void growList() {
        TimerInterruptAction[] temp = new TimerInterruptAction[actions.length*2];
        for (int i = 0, j = 0; i < actions.length && actions[i] != null; i++) {
            temp[j++] = actions[i];
        }
        actions = temp;
    }

    // profiling method?
    protected void removeTimerInterruptActionHook(int i) {
    }

    public void removeTimerInterruptAction(TimerInterruptAction tia)
	throws PragmaAtomic {
        for (int i = 0; i < index; i++) {
            if (actions[i] == tia) {
		removeTimerInterruptActionHook(i);
		actions[i]=actions[--index];
		actions[index]=null;
                break;
            }
        }
    }

    public TimerInterruptAction[] getRegisteredActions()
	throws PragmaAtomic {
        TimerInterruptAction[] temp = new TimerInterruptAction[index];
        for (int i = 0; i < index-1; i++) {
            temp[i] = actions[i];
        }
        return temp;
    }


    public long getTimerInterruptPeriod() {
        return period;
    }


    /**
     * Requests that the timer interrupt period be set to the given value.
     * The interrupt period may only be set before the timer manager is
     * started.
     * <p>The actual timer interrupt period may vary from that requested
     * due to the limitations of the underlying timer hardware. You should
     * always check the actual timer period using 
     * {@link #getTimerInterruptPeriod}, after the timer manager has been 
     * started.
     *
     * <p>This method is not thread-safe. Only one thread should control the
     * timer manager's behaviour.
     *
     * @param period the new timer interrupt period in nanoseconds
     * @return <code>true</code> if the period was changed and 
     * <code>false</code> otherwise.
     * @throws OVMError.IllegalState if the timer manager has been started
     * @see #getTimerInterruptPeriod
     *
     */
    public boolean setTimerInterruptPeriod(long period) {
        if (isStarted) {
            throw new OVMError.IllegalState("timer manager already started");
        }
        if (period <= 0 ) {  // OR not suitable in some other way
            return false;
        }
        else {
	    this.sysPeriod = period;
            this.period = period*multiplier;
            return true;
        }
    }
    
    public boolean setTimerInterruptPeriodMultiplier(long multiplier) {
        if (isStarted) {
            throw new OVMError.IllegalState("timer manager already started");
        }
        if (multiplier <= 0 ) {  // OR not suitable in some other way
            return false;
        }
        else {
	    this.multiplier = multiplier;
            this.period = period*multiplier;
            return true;
        }
    }


    /**
     * Queries if this <code>TimerManager</code> supports the setting of
     * the interrupt period.
     *
     * @return <code>true</code> if the timer manager is not yet started, else
     * <code>false</code>
     *
     *
     * @see #setTimerInterruptPeriod
     */
    public boolean canSetTimerInterruptPeriod() {
        return !isStarted;
    }
    
    protected abstract void startHook();
    protected abstract void stopHook();

    /**
     * Start the operation of the timer manager. This installs the necessary
     * low-level timer signal handler and configures the system timer to use
     * the preferred interrupt period.
     * <p>It is an error to try to start a timer manager that has been started.
     * Restarting of this timer is not supported.
     * <p>This method is not thread-safe. Only one thread should control and
     * configure the timer.
     *
     * @see #stop
     * @see #canRestart
     * @see #isRunning
     * @throws OVMError.IllegalState if the timer manager has already been
     * started.
     */
    public void start() {
        if (isStarted) {
            throw new OVMError.IllegalState("timer manager already started");
        }
	
	startHook();
        
        isStarted = true;
        em.addEventProcessor(this);
        
        d("Timer has been started with period: " + (period/1000) + " us");

        // note: we're not enabled. That job belongs to whoever is in charge
        // of bootstrapping the system.
    }


    public void stop() {
        if (!isStarted) {
            return;
        }
	stopHook();
        em.removeEventProcessor(this);
        isStopped = true;
    }

    public boolean isRunning() {
        return isStarted && !isStopped;
    }

    /**
     * @throws OVMError.IllegalState if the timer has not been stopped
     */
    public void destroy() {
        if (isRunning()) {
            throw new OVMError.IllegalState("must stop timer first");
        }
    }

    /**
     * Always returns <code>false</code> as restart is not supported.
     *
     * @return <code>false</code> 
     *
     * @see #start
     * @see #stop
     */
    public boolean canRestart() { return false; }

}









