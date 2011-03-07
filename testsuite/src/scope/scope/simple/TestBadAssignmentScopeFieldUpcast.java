package scope.scope.simple;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

// This is the same test as TestBadAssignmentScopeField except with upcasts
public class TestBadAssignmentScopeFieldUpcast {
    @DefineScope(name="a", parent=Scope.IMMORTAL)
    static abstract class X extends Mission {
        Object y1;
        @Scope(Scope.IMMORTAL) Object y2;
        static Object y3;
    }
    static class Y { }

    void foo(X x, Y y, @Scope(Scope.IMMORTAL) Y yImm) {
        x.y1 = (Object) y;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y2 = (Object) y;
        x.y2 = (Object) yImm;
    }

    void bar(@Scope(Scope.IMMORTAL) X x, Y y, @Scope(Scope.IMMORTAL) Y yImm) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y1 = (Object) y;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y2 = (Object) y;
        x.y1 = (Object) yImm;
        x.y2 = (Object) yImm;
    }

    void baz(Y y, @Scope(Scope.IMMORTAL) Y yImm) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        X.y3 = (Object) y;
        X.y3 = (Object) yImm;
    }
}
