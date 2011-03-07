package scope.scope.simple;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

import scope.scope.simple.TestBadAssignmentScopeField.X;

public class TestBadAssignmentScopeLocalUpcast {
    @DefineScope(name="a", parent=Scope.IMMORTAL)
    static abstract class X extends Mission {
        Y y1;
        @Scope(Scope.IMMORTAL) Y y2;
        static Y y3;
    }
    static class Y { }

    void foo(X x, Y y, @Scope(Scope.IMMORTAL) Y yImm) {
        Object o = (Object) x;
        o = (Object) y;
        o = (Object) x.y1;
        @Scope(Scope.IMMORTAL) Object oImm = x.y2;
        oImm = (Object) yImm;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        oImm = (Object) x;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        o = (Object) x.y2;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        o = (Object) X.y3;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        oImm = (Object) x.y1;
    }

    void bar(@Scope(Scope.IMMORTAL) X x, Y y, @Scope(Scope.IMMORTAL) Y yImm) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        Object o = (Object) x;
        o = (Object) y;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        o = (Object) x.y1;
        @Scope(Scope.IMMORTAL) Object oImm = (Object) x.y2;
        oImm = (Object) x;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        o = (Object) x.y2;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        o = (Object) X.y3;
        oImm = (Object) x.y1;
    }

    void baz(Y y, @Scope(Scope.IMMORTAL) Y yImm) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        Object o = (Object) X.y3;
    }
}
