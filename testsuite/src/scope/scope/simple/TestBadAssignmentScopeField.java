package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestBadAssignmentScopeField {
    @DefineScope(name="a", parent=IMMORTAL)
    static abstract class X extends Mission {
        Y y1;
        @Scope(IMMORTAL) Y y2;
        static Y y3;
    }
    static class Y { }

    void foo(X x, Y y, @Scope(IMMORTAL) Y yImm) {
        x.y1 = y;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y2 = y;
        x.y2 = yImm;
    }

    void bar(@Scope(IMMORTAL) X x, Y y, @Scope(IMMORTAL) Y yImm) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y1 = y;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y2 = y;
        x.y1 = yImm;
        x.y2 = yImm;
    }

    void baz(Y y, @Scope(IMMORTAL) Y yImm) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        X.y3 = y;
        X.y3 = yImm;
    }
}
