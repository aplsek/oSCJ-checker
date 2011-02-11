package level2;

import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_2;


@SCJAllowed(members=true, value=LEVEL_2)
public class SubMissionSequencer  extends MissionSequencer {

	private boolean initialized, finalized;
	
	SubMissionSequencer(PriorityParameters priorityParameters, StorageParameters storageParameters) {
		super(priorityParameters, storageParameters); 
		initialized = finalized = false;
	}

	protected Mission getNextMission() {
		if (finalized)
			return null;
		else if (initialized) {
			finalized = true;
			return new StageTwoMission();
		}
		else {
			initialized = true;
			return new StageOneMission();
		}
	}

	@Override
	protected Mission getInitialMission() {
		return null;
	}
	
}
