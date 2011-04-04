package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestBadAssignmentScopeLocalArray extends Mission {
    void foo(@Scope(IMMORTAL) Object[] os, @Scope(UNKNOWN) Object[] os2,
            @Scope(IMMORTAL) Object oImm, X x) {
        os[0] = oImm;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        os[0] = new Object();

        os2[0] = oImm;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        os2[0] = x;
    }

    void bar(Object[] os, Object o) {
        os[0] = o;
        os[0] = new Object();
    }

    @Scope("a")
    static class X { }

    void baz(@Scope(UNKNOWN) X[] xs, X x) {
        xs[0] = x;
    }
}
