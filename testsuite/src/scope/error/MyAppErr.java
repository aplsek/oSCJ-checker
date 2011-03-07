package scope.error;

import java.util.Arrays;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.ImmortalMemory;
import javax.realtime.MemoryArea;
import javax.realtime.RealtimeThread;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.MissionManager;
import javax.safetycritical.Safelet;
import javax.safetycritical.Terminal;


import javax.safetycritical.annotate.SCJRestricted;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="APP", parent=Scope.IMMORTAL)
@Scope("APP")
public class MyAppErr extends CyclicExecutive {
	static PriorityParameters p = new PriorityParameters(18);
 	static StorageParameters s = new StorageParameters(1000L, 1000L, 1000L);
 	static RelativeTime t = new RelativeTime(5,0);

 	@Override
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
		return new CyclicSchedule(new CyclicSchedule.Frame[]{new CyclicSchedule.Frame(t,handlers)});
	}

 	@SCJRestricted(INITIALIZATION)
	public MyAppErr() {
		super(p, s);
	}

	@Override
	@SCJRestricted(INITIALIZATION)
    public void initialize() {
		new PEH();
	}

	/**
	 * A method to query the maximum amount of memory needed by this
	 * mission.
	 *
	 * @return the amount of memory needed
	 */
	@Override
    public long missionMemorySize() {
		return 1420;   // MIN without printing is 430  bytes.
	}

	@SCJRestricted(INITIALIZATION)
	public void setUp() {
	}

	@SCJRestricted(CLEANUP)
	public void tearDown() {
	}

	@Override
    @SCJRestricted(CLEANUP)
	public void cleanUp() {
	}
}