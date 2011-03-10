package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

public class TestIllegalMethodScopeOverride {
    static class X {
        @SCJAllowed(SUPPORT)
        @Scope(IMMORTAL)
        Object foo(Y y) { return null; }
        @Scope(IMMORTAL)
        Object bar() { return null; }
        @Scope(IMMORTAL)
        Object baz() { return null; }
    }
    static class Y extends X {
        @Override
        @Scope(CALLER)
        Object foo(Y y) { return null; }
        @Override
        @Scope(CALLER)
        //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_SCOPE_OVERRIDE
        Object bar() { return null; }
        @Override
        @Scope(IMMORTAL)
        Object baz() { return null; }
    }
}
