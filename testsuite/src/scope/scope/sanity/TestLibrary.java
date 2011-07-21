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

@SCJAllowed(value = LEVEL_2, members = true)
public class TestLibrary {

    @Scope(IMMORTAL)
    @DefineScope(name = "X", parent = IMMORTAL)
    @SCJAllowed(value = LEVEL_2, members = true)
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }
    }

    @Scope("X")
    @DefineScope(name = "Y", parent = "X")
    @SCJAllowed(value = LEVEL_2, members = true)
    static abstract class X2 extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X2(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }
    }

    @Scope("X")
    public static class Test {

        Object f;

        @RunsIn("Y")
        public void m() {
            f.hashCode();

            f.notify();
        }
    }
}
