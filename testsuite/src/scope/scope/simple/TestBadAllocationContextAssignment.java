package scope.scope.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=Scope.IMMORTAL)
public abstract class TestBadAllocationContextAssignment extends Mission {
    @Scope("a")
    @DefineScope(name="b", parent=Scope.IMMORTAL)
    static abstract class X extends Mission {
        @Scope(Scope.IMMORTAL)
        @DefineScope(name="a", parent=Scope.IMMORTAL)
        ManagedMemory a;

        @Scope(Scope.IMMORTAL)
        @DefineScope(name="a", parent=Scope.IMMORTAL)
        ManagedMemory a2 = a;

        @Scope(Scope.IMMORTAL)
        @DefineScope(name="b", parent=Scope.IMMORTAL)
        //## checkers.scope.ScopeChecker.ERR_BAD_ALLOCATION_CONTEXT_ASSIGNMENT
        ManagedMemory b = a;
    }
}
