package scope.defineScope.simple;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;

import static javax.safetycritical.annotate.Level.SUPPORT;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

@SCJAllowed(members=true)
//## checkers.scope.DefineScopeChecker.ERR_SCHEDULABLE_NO_DEFINE_SCOPE
public abstract class TestSchedulableNoDefineScope extends PeriodicEventHandler {

    @SCJRestricted(INITIALIZATION)
    public TestSchedulableNoDefineScope(PriorityParameters priority,
            PeriodicParameters period, StorageParameters storage) {
        super(priority, period, storage);
    }

    @Override
    @SCJAllowed(SUPPORT)
    public void handleAsyncEvent() {
    }
}
