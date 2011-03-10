package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.Scope;

public class TestScopeOnVoidOrPrimitiveReturn {
    @Scope(IMMORTAL)
    //## warning: checkers.scope.ScopeRunsInChecker.ERR_SCOPE_ON_VOID_OR_PRIMITIVE_RETURN
    void foo() { }

    @Scope(IMMORTAL)
    //## warning: checkers.scope.ScopeRunsInChecker.ERR_SCOPE_ON_VOID_OR_PRIMITIVE_RETURN
    int bar() { return 0; }
}
