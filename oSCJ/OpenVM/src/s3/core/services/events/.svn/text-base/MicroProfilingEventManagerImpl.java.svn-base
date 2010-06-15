package s3.core.services.events;

import ovm.core.services.events.*;
import ovm.core.execution.Native;
import ovm.core.execution.NativeInterface;
import s3.util.PragmaNoPollcheck;
import s3.util.PragmaAtomic;

public class MicroProfilingEventManagerImpl
    extends ProfilingEventManagerImpl {

    static final class NativeHelper implements NativeInterface {
	static native void dumpProfileHisto(byte[] name,
					    int nameSize,
					    long[] histo,
					    int histoSize,
					    long overflow);
    }

    private final static EventManager instance=
	new MicroProfilingEventManagerImpl();
    public static EventManager getInstance() {
	return instance;
    }

    public static final int HISTO_SIZE=ProfilingEventManagerImpl.HISTO_SIZE;
    public static final long GRANULARITY=ProfilingEventManagerImpl.GRANULARITY;

    long[][] processorLatencyHisto=new long[processors.length][];
    long[] processorLatencyOverflow=new long[processors.length];

    public void aboutToShutdown() {
	super.aboutToShutdown();
	for (int i=0;
	     i<numProcessors;
	     ++i) {
	    String shortName=
		"evman_profile."+
		processors[i].eventProcessorShortName();
	    NativeHelper.dumpProfileHisto(shortName.getBytes(),
					  shortName.length(),
					  processorLatencyHisto[i],
					  HISTO_SIZE,
					  processorLatencyOverflow[i]);
	}
    }

    protected void callEventProcessor(int i)
	throws PragmaNoPollcheck {
	if (profilingEnabled) {
	    // the lack of a try-finally is intentional.  I don't want
	    // to record stuff in the histo if we're propagating exceptions,
	    // since an exception means that something got seriously messed
	    // up!
	    long before=currentTime();
	    super.callEventProcessor(i);
	    long latency=currentTime()-before;
	    if (latency>=HISTO_SIZE) {
		processorLatencyOverflow[i]++;
	    } else {
		processorLatencyHisto[i][(int)latency]++;
	    }
	} else {
	    super.callEventProcessor(i);
	}
    }

    protected int addEventProcessorImpl(EventProcessor handler)
	throws PragmaAtomic {
	int index=super.addEventProcessorImpl(handler);
	processorLatencyHisto[index]=new long[HISTO_SIZE];
	return index;
    }

    protected void removeEventProcessorHook(int i)
	throws PragmaAtomic {
	processorLatencyHisto[i]=processorLatencyHisto[numProcessors-1];
	processorLatencyOverflow[i]=processorLatencyOverflow[numProcessors-1];
	processorLatencyHisto[numProcessors-1]=null;
	processorLatencyOverflow[numProcessors-1]=0;
    }

    public void resetProfileHistograms() throws PragmaAtomic {
	super.resetProfileHistograms();
	for (int i=0;i<numProcessors;++i) {
	    processorLatencyOverflow[i]=0;
	    for (int j=0;j<processorLatencyHisto[i].length;++j) {
		processorLatencyHisto[i][j]=0;
	    }
	}
    }
}


