package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
public class TestBadScopeNameField {
    @Scope(IMMORTAL)
    static class X {
        //## checkers.scope.ScopeRunsInChecker.ERR_BAD_SCOPE_NAME
        @Scope("a") Object o;
    }
}
