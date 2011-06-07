package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
public class TestBadAssignmentScopeField {

    @Scope(IMMORTAL)
    @DefineScope(name="a", parent=IMMORTAL)
    @SCJAllowed(members = true)
    static abstract class MissionSeq extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public MissionSeq() {super(null, null);}

        Y y1;
        @Scope(IMMORTAL) Y y2;
        static Y y3;
    }

    @Scope("a")
    static class X {
        Y y1;
        @Scope(IMMORTAL) Y y2;
        static Y y3;

    }

    static class Z {
        Y y1;
        @Scope(IMMORTAL) Y y2;
        static Y y3;

    }

    static class Y { }

    void foo(Z z, Y y, @Scope(IMMORTAL) Y yImm) {
        z.y1 = y;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        z.y2 = y;
        z.y2 = yImm;
    }

    void bar(@Scope(IMMORTAL) Z z, Y y, @Scope(IMMORTAL) Y yImm) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        z.y1 = y;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        z.y2 = y;
        z.y1 = yImm;
        z.y2 = yImm;
    }

    void baz(Y y, @Scope(IMMORTAL) Y yImm) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        X.y3 = y;
        X.y3 = yImm;
    }
}
