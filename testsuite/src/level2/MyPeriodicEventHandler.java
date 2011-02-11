package level2;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Scope.UNKNOWN;


@SCJAllowed(members=true, value=LEVEL_2)
public class MyPeriodicEventHandler extends PeriodicEventHandler {
	private static final int _priority = 17; 
	private static final int _memSize = 5000; 
	private int _eventCounter;
	
	public MyPeriodicEventHandler(String aehName, RelativeTime startTime,
			RelativeTime period) { 
		super(new PriorityParameters(_priority),
			new PeriodicParameters(startTime, period), 
			new StorageParameters(10000, 10000, 10000));
	}
	
	public void handleAsyncEvent() {
		++_eventCounter;
	}

	public void cleanup() {}

	@Override
	public StorageParameters getThreadConfigurationParameters() {
		return null;
	}

}
