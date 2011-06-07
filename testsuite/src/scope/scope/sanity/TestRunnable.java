package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@DefineScope(name = "D", parent = IMMORTAL)
@SCJAllowed(value = LEVEL_2, members = true)
public abstract class TestRunnable extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestRunnable(PriorityParameters priority, StorageParameters storage) {
        super(priority, storage);
    }

    @Scope("D")
    @SCJAllowed(value = LEVEL_2, members = true)
    class MyRun implements Runnable {
        @Override
        public void run() {}
    }


    @Scope("D")
    @SCJAllowed(value = LEVEL_2, members = true)
    class OverrideRunnable extends MyRun {

        @Override
        public void run() {
        }
    }
}