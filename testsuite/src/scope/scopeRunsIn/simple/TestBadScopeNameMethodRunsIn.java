package scope.scopeRunsIn.simple;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

public class TestBadScopeNameMethodRunsIn {
    @Scope(Scope.IMMORTAL)
    static class X {
        @RunsIn("a")
        //## checkers.scope.ScopeRunsInChecker.ERR_BAD_SCOPE_NAME
        void foo() { }
    }
}
