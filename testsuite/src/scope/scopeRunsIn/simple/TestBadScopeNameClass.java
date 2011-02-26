package scope.scopeRunsIn.simple;

import javax.safetycritical.annotate.Scope;

public class TestBadScopeNameClass {
    @Scope("a")
    //## checkers.scope.ScopeRunsInChecker.ERR_BAD_SCOPE_NAME
    static class X { }
}
