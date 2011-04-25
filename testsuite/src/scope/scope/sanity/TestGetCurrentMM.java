package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope("D")
@SCJAllowed(value = LEVEL_2, members = true)
public class TestGetCurrentMM  {

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

        public void method() {
            @Scope("IMMORTAL")
            @DefineScope(name = "C", parent = IMMORTAL)
            ManagedMemory mm = (ManagedMemory) RealtimeThread.getCurrentMemoryArea();
        }
    }

}
