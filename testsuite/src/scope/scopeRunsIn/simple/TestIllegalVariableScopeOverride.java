package scope.scopeRunsIn.simple;

import javax.safetycritical.annotate.Scope;

public class TestIllegalVariableScopeOverride {
    @Scope(Scope.IMMORTAL)
    static class X { }
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_VARIABLE_SCOPE_OVERRIDE
    @Scope(Scope.UNKNOWN) X x1;
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_VARIABLE_SCOPE_OVERRIDE
    void foo(@Scope(Scope.CURRENT) X x2) { }
    @Scope(Scope.IMMORTAL) X x3;
}
