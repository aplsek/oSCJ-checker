package scope.scopeRunsIn.simple;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

public class TestIllegalMethodScopeOverride {
    static class X {
        @SCJAllowed(Level.SUPPORT)
        @Scope(Scope.IMMORTAL)
        void foo(Y y) { }
        @Scope(Scope.IMMORTAL)
        void bar() { }
        @Scope(Scope.IMMORTAL)
        void baz() { }
    }
    static class Y extends X {
        @Override
        @Scope(Scope.CURRENT)
        void foo(Y y) { }
        @Override
        @Scope(Scope.CURRENT)
        //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_SCOPE_OVERRIDE
        void bar() { }
        @Override
        @Scope(Scope.IMMORTAL)
        void baz() { }
    }
}
