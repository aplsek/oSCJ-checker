package scope.schedulable.simple;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
@Scope(IMMORTAL)
@DefineScope(name="Level0App", parent=IMMORTAL)
public class TestGetSchedule extends CyclicExecutive {

    @SCJRestricted(INITIALIZATION)
    public TestGetSchedule() {
        super(null);
    }

    @Override
    @SCJAllowed(SUPPORT)
    //## checkers.scope.SchedulableChecker.ERR_CYCLIC_EXEC_GET_SCHEDULE_RUNS_IN_MISMATCH
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return null;
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    @RunsIn("Level0App")
    public void initialize() {
    }

    @Override
    @SCJAllowed(SUPPORT)
    public long missionMemorySize() {
        return 5000000;
    }
}