//scope/TestEscaping2.java:34: Cannot assign expression in scope IMMORTAL to variable in scope a.
//        J j = t.j; // disallowed because foo does not run in IMMORTAL
//          ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope(IMMORTAL)
public class TestEscaping2 {
    J j;
    
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

class J {
    
}

@Scope(IMMORTAL)
class K {
    @RunsIn("a") public void foo(TestEscaping2 t) {
        J j = t.j; // disallowed because foo does not run in IMMORTAL
    }

    public void bar(TestEscaping2 t) {
        J j2 = t.j; // allowed because bar runs in IMMORTAL
    }
}