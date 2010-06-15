package s3.core.services.events;

import ovm.core.services.events.*;
import ovm.core.execution.Native;
import ovm.core.execution.NativeInterface;
import s3.util.PragmaNoPollcheck;
import s3.util.PragmaAtomic;

public class ProfilingEventManagerImpl
    extends EventManagerImpl {

    static final class NativeHelper implements NativeInterface {
	static native void dumpProfileHisto(byte[] name,
					    int nameSize,
					    long[] histo,
					    int histoSize,
					    long overflow);
    }

    private final static EventManager instance=
	new ProfilingEventManagerImpl();
    public static EventManager getInstance() {
	return instance;
    }

    public static final int HISTO_SIZE=1000;
    public static final long GRANULARITY=1000;

    long[] totalLatencyHisto=new long[HISTO_SIZE];
    long totalLatencyOverflow;

    boolean profilingEnabled = true;

    public void aboutToShutdown() {
	super.aboutToShutdown();
	String totalName="evman_profile.total";
	NativeHelper.dumpProfileHisto(totalName.getBytes(),
				      totalName.length(),
				      totalLatencyHisto,
				      HISTO_SIZE,
				      totalLatencyOverflow);
    }

    protected static long currentTime() {
	return Native.getCurrentTime()/GRANULARITY;
    }

    public void processEvents() throws PragmaNoPollcheck {
	if (profilingEnabled) {
	    // the lack of a try-finally is intentional.  I don't want
	    // to record stuff in the histo if we're propagating exceptions,
	    // since an exception means that something got seriously messed
	    // up!
	    long before=currentTime();
	    super.processEvents();
	    long latency=currentTime()-before;
	    if (latency>=HISTO_SIZE) {
		totalLatencyOverflow++;
	    } else {
		totalLatencyHisto[(int)latency]++;
	    }
	} else {
	    super.processEvents();
	}
    }
    
    public void resetProfileHistograms() throws PragmaAtomic {
	totalLatencyOverflow=0;
	for (int i=0;i<totalLatencyHisto.length;++i) {
	    totalLatencyHisto[i]=0;
	}
    }
    
    public void disableProfileHistograms() throws PragmaAtomic {
	profilingEnabled=false;
    }
}


