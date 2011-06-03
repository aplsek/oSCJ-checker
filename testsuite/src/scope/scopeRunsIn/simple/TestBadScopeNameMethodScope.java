package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
public class TestBadScopeNameMethodScope {
    @Scope(IMMORTAL)
    static class X {
        @RunsIn("a")
        //## checkers.scope.ScopeRunsInChecker.ERR_BAD_SCOPE_NAME
        void foo() { }
    }
}
