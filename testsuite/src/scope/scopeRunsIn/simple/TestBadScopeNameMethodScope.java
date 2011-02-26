package scope.scopeRunsIn.simple;

import javax.safetycritical.annotate.Scope;

public class TestBadScopeNameMethodScope {
    @Scope(Scope.IMMORTAL)
    static class X {
        @Scope("a")
        //## checkers.scope.ScopeRunsInChecker.ERR_BAD_SCOPE_NAME
        void foo() { }
    }
}
