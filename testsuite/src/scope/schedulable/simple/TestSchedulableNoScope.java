package scope.schedulable.simple;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;


import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

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
    public void handleAsyncEvent() {
    }
}
