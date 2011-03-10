package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.Scope;

public class TestIllegalClassScopeOverride {
    @Scope(IMMORTAL)
    static class X { }
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_CLASS_SCOPE_OVERRIDE
    static class Y extends X { }
    @Scope(IMMORTAL)
    static interface Z { }
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_CLASS_SCOPE_OVERRIDE
    static class W implements Z { }
}
