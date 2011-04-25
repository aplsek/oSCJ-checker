package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope("D")
@SCJAllowed(value = LEVEL_2, members = true)
public class TestRunnable implements Runnable {

    @Scope("D")
    @DefineScope(name = "D", parent = IMMORTAL)
    @SCJAllowed(value = LEVEL_2, members = true)
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }
    }

    @Scope("C")
    @DefineScope(name = "C", parent = IMMORTAL)
    @SCJAllowed(value = LEVEL_2, members = true)
    static abstract class Y extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public Y(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }
    }

    @Override
    @SCJAllowed
    public void run() {
    }
}

@SCJAllowed(value = LEVEL_2, members = true)
class OverrideRunnable extends TestRunnable {

    @Override
    @RunsIn("C")
    @SCJAllowed
    public void run() {
    }

}
