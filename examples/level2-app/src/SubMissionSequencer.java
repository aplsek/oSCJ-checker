

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true, value=LEVEL_2)
@Scope("PrimaryMission")
@DefineScope(name="StageMission", parent= "PrimaryMission")
public class SubMissionSequencer  extends MissionSequencer {

	private boolean initialized, finalized;

	@SCJRestricted(INITIALIZATION)
	SubMissionSequencer(PriorityParameters priorityParameters, StorageParameters storageParameters) {
		super(priorityParameters, storageParameters);
		initialized = finalized = false;
	}

	@Override
	@SCJAllowed(SUPPORT)
	@RunsIn("StageMission")
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
}
