package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

public class TestBadAssignmentScopeLocal {
    @DefineScope(name="a", parent=IMMORTAL)
    static abstract class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}

        Y y1;
        @Scope(IMMORTAL) Y y2;
        static Y y3;
    }
    static class Y { }

    void foo(X x, Y y, @Scope(IMMORTAL) Y yImm) {
        y = x.y1;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        y = x.y2;
        yImm = x.y2;
    }

    void bar(@Scope(IMMORTAL) X x, Y y, @Scope(IMMORTAL) Y yImm) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        y = x.y1;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        y = x.y2;
        yImm = x.y1;
        yImm = x.y2;
    }

    void baz(Y y, @Scope(IMMORTAL) Y yImm) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        y = X.y3;
        yImm = X.y3;
    }
}
