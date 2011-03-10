package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;

@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestBadGetCurrentManagedMemory extends Mission {
    @RunsIn(IMMORTAL)
    void foo() {
        //## checkers.scope.ScopeChecker.ERR_BAD_GET_CURRENT_MANAGED_MEMORY
        ManagedMemory.getCurrentManagedMemory();
    }

    @RunsIn(CALLER)
    void bar() {
        //## checkers.scope.ScopeChecker.ERR_BAD_GET_CURRENT_MANAGED_MEMORY
        ManagedMemory.getCurrentManagedMemory();
    }

    @RunsIn("a")
    void baz() {
        ManagedMemory.getCurrentManagedMemory();
    }
}
