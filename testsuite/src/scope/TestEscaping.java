//tests/scope/TestEscaping.java:13: Cannot assign expression in scope a to variable in scope IMMORTAL.
//        this.m = m; // fails because the parameter m may not reside in IMMORTAL
//               ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope(IMMORTAL)
public class TestEscaping {
    private M m;
    
    @RunsIn("a") public void foo(M m) {
        this.m = m; // fails because the parameter m may not reside in IMMORTAL
    }
    
    static class Helper {
        static void foo() {
            ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new R1());
        }
        @Scope(IMMORTAL) 
        @DefineScope(name="a", parent=IMMORTAL)
        static class R1 implements Runnable {
            @Override
            @RunsIn("a")
            public void run() {
            }
        }
    }
}

class M {
}