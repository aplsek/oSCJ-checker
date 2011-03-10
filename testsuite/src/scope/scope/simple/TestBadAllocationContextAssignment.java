package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestBadAllocationContextAssignment extends Mission {
    @Scope("a")
    @DefineScope(name="b", parent=IMMORTAL)
    static abstract class X extends Mission {
        @Scope(IMMORTAL)
        @DefineScope(name="a", parent=IMMORTAL)
        ManagedMemory a;

        @Scope(IMMORTAL)
        @DefineScope(name="a", parent=IMMORTAL)
        ManagedMemory a2 = a;

        @Scope(IMMORTAL)
        @DefineScope(name="b", parent=IMMORTAL)
        //## checkers.scope.ScopeChecker.ERR_BAD_ALLOCATION_CONTEXT_ASSIGNMENT
        ManagedMemory b = a;
    }
}
