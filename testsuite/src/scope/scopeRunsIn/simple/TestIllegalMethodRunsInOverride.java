package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

public class TestIllegalMethodRunsInOverride {
    @Scope(IMMORTAL)
    static class X {
        @SCJAllowed(SUPPORT)
        @RunsIn(IMMORTAL)
        void foo(Y y) { }
        @RunsIn(IMMORTAL)
        void bar() { }
        @RunsIn(IMMORTAL)
        Object baz() { return null; }
    }
    @Scope(IMMORTAL)
    static class Y extends X {
        @Override
        @RunsIn(CALLER)
        void foo(Y y) { }
        @Override
        @RunsIn(CALLER)
        //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_RUNS_IN_OVERRIDE
        void bar() { }
        @Override
        @RunsIn(IMMORTAL)
        Object baz() { return super.baz(); }
    }
}
