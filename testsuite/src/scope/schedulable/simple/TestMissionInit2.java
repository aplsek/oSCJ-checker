package scope.schedulable.simple;


import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Level.SUPPORT;

@SCJAllowed(members=true)
@Scope(IMMORTAL)
@DefineScope(name="a", parent=IMMORTAL)
public class TestMissionInit2 extends CyclicExecutive {

    @SCJRestricted(INITIALIZATION)
    public TestMissionInit2() {
        super(null);
    }

    @Override
    @SCJAllowed(SUPPORT)
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return null;
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    @RunsIn("a")
    public void initialize() {
        new PEH(null,null,null);

        //## checkers.scope.SchedulableChecker.ERR_SCHEDULABLE_MULTI_INIT
        new PEH(null,null,null);

        method();
    }

    @SCJRestricted(INITIALIZATION)
    private void method() {
        //## checkers.scope.SchedulableChecker.ERR_SCHED_INIT_OUT_OF_INIT_METH
        new PEH(null,null,null);
    }

    @Override
    @SCJAllowed(SUPPORT)
    @SCJRestricted(INITIALIZATION)
    public void setUp() {
    }

    @Override
    @SCJAllowed(SUPPORT)
    @SCJRestricted(CLEANUP)
    public void tearDown() {
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

}