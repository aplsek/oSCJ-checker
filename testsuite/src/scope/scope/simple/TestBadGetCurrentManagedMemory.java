package scope.scope.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=Scope.IMMORTAL)
public abstract class TestBadGetCurrentManagedMemory extends Mission {
    @RunsIn(Scope.IMMORTAL)
    void foo() {
        //## checkers.scope.ScopeChecker.ERR_BAD_GET_CURRENT_MANAGED_MEMORY
        ManagedMemory.getCurrentManagedMemory();
    }

    @RunsIn(Scope.CALLER)
    void bar() {
        //## checkers.scope.ScopeChecker.ERR_BAD_GET_CURRENT_MANAGED_MEMORY
        ManagedMemory.getCurrentManagedMemory();
    }

    @RunsIn("a")
    void baz() {
        ManagedMemory.getCurrentManagedMemory();
    }
}
