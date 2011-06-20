package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.CALLER;
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
public class TestVariable {


    @Scope(IMMORTAL)
    @DefineScope(name = "X", parent = IMMORTAL)
    @SCJAllowed(value = LEVEL_2, members = true)
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }
    }

    @SCJAllowed(members=true)
    @Scope("X")
    static class Data  {
        public int value;

        @RunsIn(CALLER)
        public int compareTo(Data o) {
            if (o == null || !(o instanceof Data))
                return -1;
            Data elem = o;
            if (this.value > elem.value)
                return +1;
            else if (this.value < elem.value)
                return -1;
            else
                return 0;
        }
    }

}


