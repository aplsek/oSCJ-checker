package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
public class TestBadAssignmentScopeFieldUnknown {
    static class X {
        Y y1;
        @Scope(UNKNOWN) Y y2;
    }
    static class Y { }

    void foo(final @Scope(UNKNOWN) X x, final Y y) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y1 = y;
        if (ManagedMemory.allocatedInSame(x, y)) {
            x.y1 = y;
        }
        if (ManagedMemory.allocatedInParent(x, y)) {
            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            x.y1 = y;
        }
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y2 = y;
        if (ManagedMemory.allocatedInSame(x, y)) {
            x.y2 = y;
        }
        if (ManagedMemory.allocatedInParent(x, y)) {
            x.y2 = y;
        }
    }

    void bar(final X x, final @Scope(UNKNOWN) Y y) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y1 = y;
        if (ManagedMemory.allocatedInSame(x, y)) {
            x.y1 = y;
        }
        if (ManagedMemory.allocatedInParent(x, y)) {
            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            x.y1 = y;
        }
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y2 = y;
        if (ManagedMemory.allocatedInSame(x, y)) {
            x.y2 = y;
        }
        if (ManagedMemory.allocatedInParent(x, y)) {
            x.y2 = y;
        }
    }
}
