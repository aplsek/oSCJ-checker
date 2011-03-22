package scope.defineScope.sanity;

import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope("A")
@DefineScope(name = "A", parent = IMMORTAL)
public class TestMissionSequencer extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestMissionSequencer(PriorityParameters priority,
            StorageParameters storage) {
        super(priority, storage);
    }

    @Override
    protected Mission getInitialMission() {
        return null;
    }

    @Override
    protected Mission getNextMission() {
        return null;
    }
}
