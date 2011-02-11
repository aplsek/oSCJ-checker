package level2;

import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PriorityScheduler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_2;

@SCJAllowed(members=true, value=LEVEL_2)
public class CleanupMission extends Mission {
		static final private int MISSION_MEMORY_SIZE = 10000; 
		static final private int PRIORITY = PriorityScheduler.instance().getNormPriority();
		
		public void initialize() {
			ManagedMemory.getCurrentManagedMemory().resize(MISSION_MEMORY_SIZE);
			PriorityParameters pp = new PriorityParameters(PRIORITY); 
			StorageParameters sp = new StorageParameters(100000L, 1000, 1000);
			MyCleanupThread t = new MyCleanupThread(pp, sp);	
		}

		@Override
		public long missionMemorySize() {
			return MISSION_MEMORY_SIZE;
		}
}