package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
public class TestBadGuardArgument {
    void foo(Object x, @Scope(UNKNOWN) Object y) {
        //## checkers.scope.ScopeChecker.ERR_BAD_GUARD_ARGUMENT
        if (ManagedMemory.allocatedInParent(x, y)) { }
        //## checkers.scope.ScopeChecker.ERR_BAD_GUARD_ARGUMENT
        if (ManagedMemory.allocatedInSame(x.getClass(), y)) { }
    }
}
