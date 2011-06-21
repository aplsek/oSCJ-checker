package scope.schedulable.simple;

import static checkers.scope.SchedulableChecker.ERR_MISSION_INIT_RUNS_IN_MISMATCH;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.Safelet;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;


@Scope(IMMORTAL)
@SCJAllowed(members=true)
@DefineScope(name="A", parent=IMMORTAL)
public class TestMissionSafelet extends Mission implements Safelet {

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    public MissionSequencer getSequencer() {
        return null;
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    public void setUp() {
    }

    @SCJRestricted(CLEANUP)
    @SCJAllowed(SUPPORT)
    @Override
    public void tearDown() {
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    //## checkers.scope.SchedulableChecker.ERR_MISSION_INIT_RUNS_IN_MISMATCH
    protected void initialize() {
    }

}
