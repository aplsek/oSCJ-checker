package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.safetycritical.annotate.Scope;

public class TestIllegalVariableScopeOverride {
    @Scope(IMMORTAL)
    static class X { }
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_VARIABLE_SCOPE_OVERRIDE
    @Scope(UNKNOWN) X x1;
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_VARIABLE_SCOPE_OVERRIDE
    void foo(@Scope(CALLER) X x2) { }
    @Scope(IMMORTAL) X x3;
}
