package scope.schedulable.simple;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;


import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestSchedulableNoScope extends PeriodicEventHandler {

    @SCJRestricted(INITIALIZATION)
    public TestSchedulableNoScope(PriorityParameters priority,
            PeriodicParameters period, StorageParameters storage) {
        super(priority, period, storage);
    }

    @Override
    public void handleAsyncEvent() {
    }
}
