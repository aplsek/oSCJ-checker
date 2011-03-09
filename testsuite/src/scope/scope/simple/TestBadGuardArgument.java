package scope.scope.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.Scope;

public class TestBadGuardArgument {
    void foo(Object x, @Scope(Scope.UNKNOWN) Object y) {
        //## checkers.scope.ScopeChecker.ERR_BAD_GUARD_ARGUMENT
        if (ManagedMemory.allocInParent(x, y)) { }
        //## checkers.scope.ScopeChecker.ERR_BAD_GUARD_ARGUMENT
        if (ManagedMemory.allocInSame(x.getClass(), y)) { }
    }
}
