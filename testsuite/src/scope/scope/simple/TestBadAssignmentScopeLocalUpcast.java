package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

public class TestBadAssignmentScopeLocalUpcast {
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
        Object o = x;
        o = y;
        o = x.y1;
        @Scope(IMMORTAL) Object oImm = x.y2;
        oImm = yImm;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        oImm = x;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        o = x.y2;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        o = X.y3;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        oImm = x.y1;
    }

    void bar(@Scope(IMMORTAL) X x, Y y, @Scope(IMMORTAL) Y yImm) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        Object o = x;
        o = y;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        o = x.y1;
        @Scope(IMMORTAL) Object oImm = x.y2;
        oImm = x;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        o = x.y2;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        o = X.y3;
        oImm = x.y1;
    }

    void baz(Y y, @Scope(IMMORTAL) Y yImm) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        Object o = X.y3;
    }
}
