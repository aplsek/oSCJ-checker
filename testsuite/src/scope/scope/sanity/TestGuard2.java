package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;


@Scope(IMMORTAL)
@DefineScope(name="a", parent=IMMORTAL)
@SCJAllowed(value = LEVEL_2, members = true)
public abstract class TestGuard2 extends MissionSequencer {
    A a;

    @SCJRestricted(INITIALIZATION)
    public TestGuard2() {super(null, null);}

    @RunsIn(CALLER)
    public void setA(final A a, @Scope(UNKNOWN) final B b) {
        if (ManagedMemory.allocatedInSame(a, b))
            a.f = b; // DYNAMIC GUARD


    }

    @RunsIn(CALLER)
    public void setX(final X x, @Scope(UNKNOWN) final B b) {
        if (ManagedMemory.allocatedInSame(x, b)) {
            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            x.a.f = b; // DYNAMIC GUARD
        }
    }

    static class X {
        A a;
    }

    static class A {
        B f;
    }

    static class B {

    }
}