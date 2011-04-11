

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true, value=LEVEL_2)
@Scope(IMMORTAL)
public class MainMissionSequencer extends MissionSequencer {

    private boolean initialized, finalized;

    @SCJRestricted(INITIALIZATION)
    MainMissionSequencer(PriorityParameters priorityParameters,
            StorageParameters storageParameters) {
        super(priorityParameters, storageParameters);
        initialized = finalized = false;

    }

    @Override
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

}
