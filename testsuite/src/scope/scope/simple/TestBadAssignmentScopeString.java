package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestBadAssignmentScopeString extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadAssignmentScopeString() {super(null, null);}

    @RunsIn("a")
    void foo() {
        // String literals are allocated in IMMORTAL
        @Scope(IMMORTAL) String s1 = "test";
        // String literals added together are still compile-time strings, and
        // therefore allocated in IMMORTAL
        @Scope(IMMORTAL) String s2 = "test" + "test2" + "test3";
        String s3 = new String("test");
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        String s4 = "test";
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        String s5 = "test" + "test2" + "test3";
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        @Scope(IMMORTAL) String s6 = new String("test");
    }
}
