
package s3.core.services.timer;

import ovm.core.execution.Native;
import ovm.core.execution.NativeInterface;
import ovm.core.services.events.EventManager;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.process.ForkManager;
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
import s3.util.queues.DelayableSingleLinkDeltaElement;
import s3.util.queues.SingleLinkDeltaQueue;
import s3.util.queues.SingleLinkTimerQueue;

public class NanosleepTimerManagerImpl extends BaseTimerManagerImpl {

    private static final class Helper implements NativeInterface {
        static native int initNanosleepTimer(long period);
    }

    /**
     * This is a singleton class. The instance can be accessed from here
     * or more usually via the service configurator.
     */
    protected static TimerManager instance = new NanosleepTimerManagerImpl();

    /**
     * Returns the singleton instance of this class
     * @return the singleton instance of this class
     */
    public static TimerManager getInstance() {
        return instance;
    }

    protected volatile int lastCount;

    /**
     * Create a timer manager
     * using the default initial action list size.
     *
     *
     */
    public NanosleepTimerManagerImpl() {}

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
    public NanosleepTimerManagerImpl(int size) {
	super(size);
    }

    protected int getCurrentCount() {
	return (int)(Native.getCurrentTime()/period);
    }

    private ForkManager.AfterHandler afterForkHandler =
        new ForkManager.AfterHandler() {
            public void afterInChild() {
                Helper.initNanosleepTimer(period);
            }
            public void afterInParent(int pid) {
		// thread should still be running.
                //Helper.initNanosleepTimer(period);
            }
        };

    protected void startHook() {
	if (!em.isStarted()) em.start(); /* need event manager to be started if we're
					    going to be starting threads. */
        Helper.initNanosleepTimer(period);
        ForkManager.addAfter(afterForkHandler);
    }

    protected void stopHook() {
        ForkManager.removeAfter(afterForkHandler);
    }

    protected void initHook() {
    }
    

}


