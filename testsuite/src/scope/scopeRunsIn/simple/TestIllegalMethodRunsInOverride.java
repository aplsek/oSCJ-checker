package scope.scopeRunsIn.simple;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

public class TestIllegalMethodRunsInOverride {
    @Scope(Scope.IMMORTAL)
    static class X {
        @SCJAllowed(Level.SUPPORT)
        @RunsIn(Scope.IMMORTAL)
        void foo(Y y) { }
        @RunsIn(Scope.IMMORTAL)
        void bar() { }
        @RunsIn(Scope.IMMORTAL)
        Object baz() { return null; }
    }
    @Scope(Scope.IMMORTAL)
    static class Y extends X {
        @Override
        @RunsIn(Scope.CURRENT)
        void foo(Y y) { }
        @Override
        @RunsIn(Scope.CURRENT)
        //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_RUNS_IN_OVERRIDE
        void bar() { }
        @Override
        @RunsIn(Scope.IMMORTAL)
        Object baz() { return super.baz(); }
    }
}
