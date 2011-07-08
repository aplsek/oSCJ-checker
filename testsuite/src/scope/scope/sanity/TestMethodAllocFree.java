package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.*;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
public class TestMethodAllocFree {

    @Scope(IMMORTAL)
    @DefineScope(name = "X", parent = IMMORTAL)
    @SCJAllowed(members=true)
    static abstract class MS extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public MS(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }
    }

    @Scope("X")
    @DefineScope(name = "Y", parent = "X")
    @SCJAllowed(members=true)
    static abstract class MS2 extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public MS2(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }
    }

    @SCJAllowed(members=true)
    @Scope("X")
    static class X  {
        public int value;

        @RunsIn(THIS)
        @SCJRestricted(mayAllocate=false)
        public int getValue() {
            return value;
        }
    }

    @SCJAllowed(members=true)
    @Scope("Y")
    static class Y  {
        X x;

        void m () {
            x.getValue();           // OK
        }
    }
}


