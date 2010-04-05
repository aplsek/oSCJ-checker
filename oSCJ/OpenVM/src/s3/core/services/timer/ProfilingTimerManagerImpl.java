package s3.core.services.timer;

import ovm.core.execution.*;
import ovm.core.services.timer.*;

import s3.util.PragmaAtomic;
import s3.util.PragmaNoPollcheck;

public class ProfilingTimerManagerImpl
    extends TimerManagerImpl {
    
    private static final class NativeHelper implements NativeInterface {
	static native void dumpProfileHisto(byte[] name,
					    int nameSize,
					    long[] histo,
					    int histoSize,
					    long overflow);
    }

    private final static TimerManager instance=
	new ProfilingTimerManagerImpl();
    public static TimerManager getInstance() {
	return instance;
    }
    
    public static final int HISTO_SIZE=1000;
    public static final long GRANULARITY=1000;

    long[] totalLatencyHisto=new long[HISTO_SIZE];
    long totalLatencyOverflow;

    long[][] actionLatencyHisto;
    long[] actionLatencyOverflow;

    public ProfilingTimerManagerImpl() {
	this(DEFAULT_ACTION_LIST_SIZE);
    }

    public ProfilingTimerManagerImpl(int size) {
	super(size);
	if (size <= 0) {
	    size = DEFAULT_ACTION_LIST_SIZE;
	}
	actionLatencyHisto=new long[size][];
	actionLatencyOverflow=new long[size];
    }

    static long currentTime() throws PragmaNoPollcheck {
	return Native.getCurrentTime()/GRANULARITY;
    }

    public void aboutToShutdown() {
	super.aboutToShutdown();
	String totalName="timer_profile.total";
	NativeHelper.dumpProfileHisto(totalName.getBytes(),
				      totalName.length(),
				      totalLatencyHisto,
				      HISTO_SIZE,
				      totalLatencyOverflow);

	for (int i=0;
	     i<index;
	     ++i) {
	    String name="timer_profile."+
		actions[i].timerInterruptActionShortName();
	    NativeHelper.dumpProfileHisto(name.getBytes(),
					  name.length(),
					  actionLatencyHisto[i],
					  HISTO_SIZE,
					  actionLatencyOverflow[i]);
	}
    }


    void processActions(int ticks) throws PragmaNoPollcheck {
	// the lack of a try-finally is intentional.  I don't want
	// to record stuff in the histo if we're propagating exceptions,
	// since an exception means that something got seriously messed
	// up!
	long before=currentTime();
	super.processActions(ticks);
	long latency=currentTime()-before;
	if (latency>=HISTO_SIZE) {
	    totalLatencyOverflow++;
	} else {
	    totalLatencyHisto[(int)latency]++;
//             Native.print_string("timer_profile measured ");
//             Native.print_int((int)latency);
//             Native.print_string("\n");
	}
    }

    protected void processAction(int ticks, int i) throws PragmaNoPollcheck {
	long before=currentTime();
	super.processAction(ticks,i);
	long latency=currentTime()-before;
	if (latency>=HISTO_SIZE) {
	    actionLatencyOverflow[i]++;
	} else {
	    actionLatencyHisto[i][(int)latency]++;
	}
    }

    protected void addTimerInterruptActionImpl(TimerInterruptAction tia)
	throws PragmaAtomic {
	super.addTimerInterruptActionImpl(tia);
	// lists will have been resized appropriately
	actionLatencyHisto[index-1]=new long[HISTO_SIZE];
    }

    protected void growList() {
	super.growList();

	long[][] newArrayArray=
	    new long[actions.length][];
	System.arraycopy(actionLatencyHisto,0,
			 newArrayArray,0,
			 actionLatencyHisto.length);
	actionLatencyHisto=newArrayArray;
	
	long[] newArray=
	    new long[actions.length];
	System.arraycopy(actionLatencyOverflow,0,
			 newArray,0,
			 actionLatencyOverflow.length);
	actionLatencyOverflow=newArray;
    }

    protected void removeTimerInterruptActionHook(int i) {
	actionLatencyHisto[i]=actionLatencyHisto[index-1];
	actionLatencyOverflow[i]=actionLatencyOverflow[index-1];
	actionLatencyHisto[index-1]=null;
	actionLatencyOverflow[index-1]=0;
    }
}


