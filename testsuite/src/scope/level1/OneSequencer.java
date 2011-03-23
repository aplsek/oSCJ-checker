package scope.level1;

import javax.safetycritical.annotate.RunsIn;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;

import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;

@Scope(IMMORTAL)
@SCJAllowed(value=LEVEL_1, members=true)
public class OneSequencer extends MissionSequencer {
    @SCJRestricted(INITIALIZATION)
    OneSequencer(PriorityParameters p, StorageParameters s) {
        super(p, s);
    }

    @Override
    @RunsIn("OneMission") @Scope("OneMission")
    @SCJAllowed(SUPPORT)
    protected Mission getNextMission() {
        return new OneMission();
    }
}
