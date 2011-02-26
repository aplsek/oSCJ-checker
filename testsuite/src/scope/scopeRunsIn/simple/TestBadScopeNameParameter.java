package scope.scopeRunsIn.simple;

import javax.safetycritical.annotate.Scope;

public class TestBadScopeNameParameter {
    @Scope(Scope.IMMORTAL)
    static class X {
        //## checkers.scope.ScopeRunsInChecker.ERR_BAD_SCOPE_NAME
        void foo(@Scope("a") Object o) { }
    }
}
