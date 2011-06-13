package scope.scope.simple;

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
import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.CALLER;

@SCJAllowed(value = LEVEL_2, members = true)
@Scope("D")
public class TestCompoundAssignment {

    String test = "test";

    @Scope(IMMORTAL)
    @DefineScope(name = "D", parent = IMMORTAL)
    @SCJAllowed(value = LEVEL_2, members = true)
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }
    }

    @Scope(IMMORTAL)
    @DefineScope(name = "C", parent = IMMORTAL)
    @SCJAllowed(value = LEVEL_2, members = true)
    static abstract class Y extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public Y(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }
    }

    @RunsIn(CALLER)
    void method(String str) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        test += str;
    }

    @RunsIn("D")
    void method2(String str) {
        test += str;
    }


}
