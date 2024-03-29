package scope.schedulable.simple;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;

@SCJAllowed(members=true)
@Scope(IMMORTAL)
@DefineScope(name = "a", parent = IMMORTAL)
public abstract class TestSchedulableRunsInMismatch extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestSchedulableRunsInMismatch(PriorityParameters priority,
            StorageParameters storage) {
        super(priority, storage);
    }

    @SCJAllowed(members=true)
    @Scope("a")
    @DefineScope(name = "b", parent = "a")
    //## checkers.scope.SchedulableChecker.ERR_SCHEDULABLE_RUNS_IN_MISMATCH
    public static abstract class PEH extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public PEH(PriorityParameters priority,
                PeriodicParameters period, StorageParameters storage) {
            super(priority, period, storage);
        }

        @Override
        @RunsIn("c")
        @SCJAllowed(SUPPORT)
        public void handleAsyncEvent() {
        }
    }

    @SCJAllowed(members=true)
    @Scope(IMMORTAL)
    @DefineScope(name = "c", parent = IMMORTAL)
    abstract static class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }}
}