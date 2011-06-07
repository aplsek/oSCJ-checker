package scope.schedulable.simple;

import static checkers.scope.SchedulableChecker.ERR_SCHED_INIT_OUT_OF_INIT_METH;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Level.SUPPORT;

@SCJAllowed(members=true)
@Scope("a")
public class TestMissionInit extends Mission {

    @SCJRestricted(INITIALIZATION)
    public TestMissionInit() {
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    public void initialize() {
        new PEH(null,null,null);
        new PEH(null,null,null);

        method();
    }

    @SCJRestricted(INITIALIZATION)
    private void method() {
        //## checkers.scope.SchedulableChecker.ERR_SCHED_INIT_OUT_OF_INIT_METH
        new PEH(null,null,null);
    }

    @Scope("a")
    @SCJAllowed(members=true)
    @DefineScope(name = "b", parent = "a")
    public class PEH extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public PEH(PriorityParameters priority,
                PeriodicParameters period, StorageParameters storage) {
            super(priority, period, storage);
        }

        @Override
        @RunsIn("b")
        @SCJAllowed(SUPPORT)
        public void handleAsyncEvent() {
        }
    }


    @Override
    public long missionMemorySize() {
        return 0;
    }

    @Scope(IMMORTAL)
    @DefineScope(name = "a", parent = IMMORTAL)
    @SCJAllowed(members = true)
    public class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("a")
        protected Mission getNextMission() {
            return null;
        }
    }

    @SCJAllowed(members=true)
    @Scope("a")
    class TestMissionErr extends Mission {

        @Override
        public long missionMemorySize() {
            return 0;
        }

        @Override
        @RunsIn("b")
        @SCJAllowed(SUPPORT)
        //## checkers.scope.SchedulableChecker.ERR_MISSION_INIT_RUNS_IN_MISMATCH
        protected void initialize() {
        }

    }

}