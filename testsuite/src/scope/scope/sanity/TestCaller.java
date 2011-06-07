package scope.scope.sanity;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import static javax.safetycritical.annotate.Level.SUPPORT;

import javax.realtime.AbsoluteTime;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@DefineScope(name = "D", parent = IMMORTAL)
@SCJAllowed(members = true)
public class TestCaller extends MissionSequencer {

    AbsoluteTime update_time;
    AbsoluteTime prior_update_time;

    @SCJRestricted(INITIALIZATION)
    public TestCaller() {super(null, null);}

    // called periodically by the GPS Driver
    @RunsIn(CALLER)
    synchronized void updatePosition(@Scope(CALLER) AbsoluteTime time_stamp) {
        update_time.set(time_stamp);

        update_time.set(0, 0);
    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("D")
    protected Mission getNextMission() {
        return null;
    }
}
