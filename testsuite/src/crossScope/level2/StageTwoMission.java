package crossScope.level2;

import javax.realtime.RelativeTime;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_2;

@SCJAllowed(members=true, value=LEVEL_2)
public class StageTwoMission extends Mission {
	private static final int MISSION_MEMORY_SIZE = 10000;
	
	public void initialize() {
		ManagedMemory.getCurrentManagedMemory().resize(MISSION_MEMORY_SIZE);
		(new MyPeriodicEventHandler("stage2.eh1", new RelativeTime(0, 0),
				new RelativeTime(500, 0))).register();
	}

	public long missionMemorySize() {
		return MISSION_MEMORY_SIZE;
	}
}
