package level2;

import javax.realtime.RelativeTime;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;

public class StageOneMission extends Mission {
	private static final int MISSION_MEMORY_SIZE = 10000;
	
	public void initialize() {
		ManagedMemory.getCurrentManagedMemory().resize(MISSION_MEMORY_SIZE);
		(new MyPeriodicEventHandler("stage1.eh1", new RelativeTime(0, 0),
				new RelativeTime(1000, 0))).register();
	}

	public long missionMemorySize() {
		return MISSION_MEMORY_SIZE;
	}
}
