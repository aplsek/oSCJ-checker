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
    }

    @RunsIn(CALLER)
    public void setA2(@Scope(UNKNOWN) final A aa) {
        //## ERROR.... a is not "final"
        if (ManagedMemory.allocInSame(a, aa))           // ERROR
            //## ERROR...
            a = aa; // DYNAMIC GUARD
    }

    static class A {}
}