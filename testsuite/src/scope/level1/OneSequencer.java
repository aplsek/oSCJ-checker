package scope.level1;

import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;

@Scope(IMMORTAL)
@SCJAllowed(value = LEVEL_1, members = true)
public class OneSequencer extends MissionSequencer {
    OneSequencer(PriorityParameters p, StorageParameters s) {
        super(p, s);
    }

    @Override
    @RunsIn(UNKNOWN)
    protected Mission getNextMission() {
        return new OneMission();
    }

    @Override
    protected Mission getInitialMission() {
        return null;
    }
}