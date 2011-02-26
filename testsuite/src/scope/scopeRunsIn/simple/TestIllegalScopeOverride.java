package scope.scopeRunsIn.simple;

import javax.safetycritical.annotate.Scope;

public class TestIllegalScopeOverride {
    @Scope(Scope.IMMORTAL)
    static class X { }
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_SCOPE_OVERRIDE
    @Scope(Scope.UNKNOWN) X x1;
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_SCOPE_OVERRIDE
    @Scope(Scope.CURRENT) X x2;
    @Scope(Scope.IMMORTAL) X x3;
}
