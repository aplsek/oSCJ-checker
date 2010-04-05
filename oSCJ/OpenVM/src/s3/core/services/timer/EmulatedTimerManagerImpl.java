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
import ovm.core.Executive;

import s3.util.queues.*;
import s3.util.PragmaAtomic;
import s3.util.PragmaNoPollcheck;
import s3.services.threads.TimedSuspensionOVMThreadImpl;

public class EmulatedTimerManagerImpl
    extends BaseTimerManagerImpl
    implements EventManager.WaitingAuditor {
    
    private static final class Helper implements NativeInterface {
	static native void initPollcheckTimer(short maxCount,
					      long period);
    }

    static final boolean DEBUG = false;
    static final boolean PRINT_MISSED = false;

    long lastCount;
    short maxCount=500;
    
    protected static TimerManager instance=new EmulatedTimerManagerImpl();
    
    public static TimerManager getInstance() { return instance; }

    public EmulatedTimerManagerImpl() {
	this(DEFAULT_ACTION_LIST_SIZE);
    }
    
    public EmulatedTimerManagerImpl(int size) {
	super(size);

	PollcheckManager.setSettings(new PollcheckManager.Settings(){
		public String fastPathInC() {
		    return "(eventCount--<=0)";
		}
		public String slowPathInC() {
		    return "eventPollcheckTimer()";
		}
		public boolean supportsMaxCount() { return true; }
		public void setMaxCount(short maxCount) {
		    EmulatedTimerManagerImpl.this.maxCount=maxCount;
		}
	    });
    }
    
    protected void initHook() {}

    protected int getCurrentCount() {
	return (int)(Native.getCurrentTime()/period);
    }
    
    public long overrideWaitTime(long waitTime) {
	return 0; /* wait the event manager go into a polling loop whenever
		     no threads are ready */
    }
    
    protected void startHook() {
	lastCount=Native.getCurrentTime()/period;
	em.addWaitingAuditor(this);
	Helper.initPollcheckTimer(maxCount,period);
    }
    
    protected void stopHook() {
	em.removeWaitingAuditor(this);
    }
    
}

