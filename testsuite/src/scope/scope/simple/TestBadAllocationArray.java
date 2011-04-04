package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestBadAllocationArray extends Mission {
    @Scope("a")
    static class X { }
    @RunsIn("a")
    void foo() {
        X[] x = new X[0];
    }
    @RunsIn(IMMORTAL)
    void bar() {
        //## checkers.scope.ScopeChecker.ERR_BAD_ALLOCATION_ARRAY
        X[] x = new X[0];
    }
}
