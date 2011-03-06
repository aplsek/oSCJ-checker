package scope.scopeRunsIn.simple;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

public class TestIllegalMethodScopeOverride {
    static class X {
        @SCJAllowed(Level.SUPPORT)
        @Scope(Scope.IMMORTAL)
        Object foo(Y y) { return null; }
        @Scope(Scope.IMMORTAL)
        Object bar() { return null; }
        @Scope(Scope.IMMORTAL)
        Object baz() { return null; }
    }
    static class Y extends X {
        @Override
        @Scope(Scope.CURRENT)
        Object foo(Y y) { return null; }
        @Override
        @Scope(Scope.CURRENT)
        //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_SCOPE_OVERRIDE
        Object bar() { return null; }
        @Override
        @Scope(Scope.IMMORTAL)
        Object baz() { return null; }
    }
}
