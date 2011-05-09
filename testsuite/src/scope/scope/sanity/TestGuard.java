package scope.scope.sanity;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;


@Scope("a")
@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestGuard extends MissionSequencer {
    A a;

    @SCJRestricted(INITIALIZATION)
    public TestGuard() {super(null, null);}

    @RunsIn(CALLER)
    public void setA(@Scope(UNKNOWN) final A aa) {
        if (ManagedMemory.allocatedInSame(this, aa))
            a = aa; // DYNAMIC GUARD

        //## checkers.scope.ScopeChecker.ERR_BAD_GUARD_ARGUMENT
        if (ManagedMemory.allocatedInSame(a, aa))
            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            a = aa; // DYNAMIC GUARD

    }

    static class A { }
}