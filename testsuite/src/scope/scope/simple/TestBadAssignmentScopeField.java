package scope.scope.simple;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestBadAssignmentScopeField {
    @DefineScope(name="a", parent=Scope.IMMORTAL)
    static class X {
        Y y1;
        @Scope(Scope.IMMORTAL) Y y2;
    }
    static class Y {
    }

    void foo(X x, Y y, @Scope(Scope.IMMORTAL) Y yImm) {
        x.y1 = y;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y2 = y;
        x.y2 = yImm;
    }

    void bar(@Scope(Scope.IMMORTAL) X x, Y y, @Scope(Scope.IMMORTAL) Y yImm) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y1 = y;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y2 = y;
        x.y1 = yImm;
        x.y2 = yImm;
    }
}
