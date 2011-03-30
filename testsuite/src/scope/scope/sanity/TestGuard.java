package scope.scope.sanity;

import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;


@Scope("a")
@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestGuard extends Mission {
    A a;

    @RunsIn(CALLER)
    public void setA(@Scope(UNKNOWN) final A aa) {
        if (ManagedMemory.allocInSame(this, aa))
            a = aa; // DYNAMIC GUARD

        //## checkers.scope.ScopeChecker.ERR_BAD_GUARD_ARGUMENT
        if (ManagedMemory.allocInSame(a, aa))
            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            a = aa; // DYNAMIC GUARD

    }

    static class A { }
}