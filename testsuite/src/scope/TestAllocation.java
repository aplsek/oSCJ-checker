//scope/TestAllocation.java:13: Object allocation in a context (immortal) other than its designated scope (a).
//        new C();
//        ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("immortal")
public class TestAllocation {
    
    public void foo() {
        new C();
        ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new /*@DefineScope(name = "a", parent = "immortal")*/ R());
    }
    
    @Scope("immortal") @RunsIn("a")
    public static class R implements Runnable {
        public void run() {
            new C();
        }
    }
}


@Scope("a")
class C {
}