package scope.scopeRunsIn.simple;

import javax.safetycritical.annotate.Scope;

public class TestIllegalClassScopeOverride {
    @Scope(Scope.IMMORTAL)
    static class X { }
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_CLASS_SCOPE_OVERRIDE
    static class Y extends X { }
    @Scope(Scope.IMMORTAL)
    static interface Z { }
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_CLASS_SCOPE_OVERRIDE
    static class W implements Z { }
}
