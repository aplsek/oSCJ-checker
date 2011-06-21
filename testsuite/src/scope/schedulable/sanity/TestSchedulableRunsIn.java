package scope.schedulable.sanity;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Level.SUPPORT;

@DefineScope(name = "a", parent = IMMORTAL)
@Scope(IMMORTAL)
@SCJAllowed(members=true)
public abstract class TestSchedulableRunsIn extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestSchedulableRunsIn(PriorityParameters priority,
            StorageParameters storage) {
        super(priority, storage);
    }

    @Scope("a")
    @DefineScope(name = "b", parent = "a")
    @SCJAllowed(members=true)
    public static  abstract class PEH extends PeriodicEventHandler {

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
}