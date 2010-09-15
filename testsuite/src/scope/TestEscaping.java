//tests/scope/TestEscaping.java:13: Cannot assign expression in scope a to variable in scope immortal.
//        this.m = m; // fails because the parameter m may not reside in immortal
//               ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("immortal")
public class TestEscaping {
    private M m;
    
    @RunsIn("a") public void foo(M m) {
        this.m = m; // fails because the parameter m may not reside in immortal
    }
    
    static class Helper {
        static void foo() {
            ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new /*@DefineScope(name="a", parent="immortal")*/ R1());
        }
        @Scope("immortal") @RunsIn("a")
        static class R1 implements Runnable {
            @Override
            public void run() {
            }
        }
    }
}

class M {
}