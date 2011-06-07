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

@SCJAllowed(members=true)
@Scope(IMMORTAL)
@DefineScope(name = "a", parent = IMMORTAL)
public abstract class TestSchedulableMismatch extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestSchedulableMismatch(PriorityParameters priority,
            StorageParameters storage) {
        super(priority, storage);
    }

    @Scope("a")
    @DefineScope(name = "b", parent = "a")
    @SCJAllowed(members=true)
    //## checkers.scope.SchedulableChecker.ERR_SCHEDULABLE_NO_RUNS_IN
    static abstract class PEH extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public PEH(PriorityParameters priority,
                PeriodicParameters period, StorageParameters storage) {
            super(priority, period, storage);
        }

        @Override
        @SCJAllowed(SUPPORT)
        public void handleAsyncEvent() {
        }
    }

}