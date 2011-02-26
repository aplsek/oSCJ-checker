package scope.scopeRunsIn.simple;

import javax.safetycritical.annotate.Scope;

public class TestBadScopeNameField {
    @Scope(Scope.IMMORTAL)
    static class X {
        //## checkers.scope.ScopeRunsInChecker.ERR_BAD_SCOPE_NAME
        @Scope("a") Object o;
    }
}
