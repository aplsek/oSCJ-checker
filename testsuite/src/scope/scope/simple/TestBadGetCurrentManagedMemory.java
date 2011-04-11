package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;

@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestBadGetCurrentManagedMemory extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadGetCurrentManagedMemory() {super(null, null);}

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
