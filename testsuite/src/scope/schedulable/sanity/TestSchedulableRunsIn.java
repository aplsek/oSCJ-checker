package scope.schedulable.sanity;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;


@DefineScope(name = "a", parent = IMMORTAL)
public abstract class TestSchedulableRunsIn extends Mission {

    @Scope("a")
    @DefineScope(name = "b", parent = "a")
    public abstract class PEH extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public PEH(PriorityParameters priority,
                PeriodicParameters period, StorageParameters storage) {
            super(priority, period, storage);
        }

        @Override
        @RunsIn("b")
        public void handleAsyncEvent() {
        }
    }
}