package scope.schedulable.simple;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;


import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@SCJAllowed(members=true)
@DefineScope(name="a", parent=IMMORTAL)
//## checkers.scope.SchedulableChecker.ERR_SCHEDULABLE_NO_SCOPE
public abstract class TestSchedulableNoScope extends PeriodicEventHandler {

    @SCJRestricted(INITIALIZATION)
    public TestSchedulableNoScope(PriorityParameters priority,
            PeriodicParameters period, StorageParameters storage) {
        super(priority, period, storage);
    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("a")
    public void handleAsyncEvent() {
    }


    @SCJAllowed(members=true)
    @Scope("a")
    @DefineScope(name="b", parent=IMMORTAL)
    //## checkers.scope.SchedulableChecker.ERR_SCHEDULABLE_SCOPE_DEFINESCOPE_MISMATCH
    public abstract class MySchedulable extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public MySchedulable(PriorityParameters priority,
                PeriodicParameters period, StorageParameters storage) {
            super(priority, period, storage);
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("a")
        public void handleAsyncEvent() {
        }
    }


    @SCJAllowed(members=true)
    @Scope(IMMORTAL)
    @DefineScope(name="c", parent=IMMORTAL)
    //## checkers.scope.SchedulableChecker.ERR_SCHEDULABLE_RUNS_IN_MISMATCH
    public abstract class MySchedulable2 extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public MySchedulable2(PriorityParameters priority,
                PeriodicParameters period, StorageParameters storage) {
            super(priority, period, storage);
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("b")
        public void handleAsyncEvent() {
        }
    }

}
