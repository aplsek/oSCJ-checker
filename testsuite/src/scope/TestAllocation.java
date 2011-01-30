//scope/TestAllocation.java:13: Object allocation in a context (IMMORTAL) other than its designated scope (a).
//        new C();
//        ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope(IMMORTAL)
public class TestAllocation {
    
    public void foo() {
        new C();
        ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new R());
    }
    
    @Scope(IMMORTAL) 
    @DefineScope(name = "a", parent = IMMORTAL)
    public static class R implements Runnable {
        @RunsIn("a")
        public void run() {
            new C();
        }
    }
}

@Scope("a")
class C {
}