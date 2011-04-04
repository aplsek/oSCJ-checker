package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
@Scope("a")
public abstract class TestIllegalFieldScopeArray extends Mission {
    X[] x;
    @Scope("a")
    static class X { }
    @Scope(IMMORTAL)
    static class Y {
        //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_FIELD_SCOPE
        X[] x;
    }
}
