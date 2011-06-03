package scope.scopeRunsIn.sanity;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.SCJAllowed;


@SCJAllowed(members=true)
public class TestBadScopeNameField {
    @Scope(IMMORTAL)
    static class X {
        //## checkers.scope.ScopeRunsInChecker.ERR_BAD_SCOPE_NAME
        @Scope("a") Object o;
    }
}
