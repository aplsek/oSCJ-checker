package crossScope_A.level2;

import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;

@SCJAllowed(members=true, value=LEVEL_2)
public class MainMissionSequencer extends MissionSequencer {

	private boolean initialized, finalized;
	
	MainMissionSequencer(PriorityParameters priorityParameters, 
			StorageParameters storageParameters) {
		super(priorityParameters, storageParameters); 
		initialized = finalized = false;
	
	}
		
	@SCJAllowed(SUPPORT) 
	protected Mission getNextMission() {
		if (finalized) 
			return null;
		else if (initialized) { 
			finalized = true;
			return new CleanupMission();
		} else {
			initialized = true; 
			return new PrimaryMission();
		}
	}

	@Override
	protected Mission getInitialMission() {
		return null;
	}
	
}
