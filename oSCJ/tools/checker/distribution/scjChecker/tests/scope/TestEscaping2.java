//scope/TestEscaping2.java:34: Cannot assign expression in scope immortal to variable in scope a.
//        J j = t.j; // disallowed because foo does not run in immortal
//          ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("immortal")
public class TestEscaping2 {
    J j;
    
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

class J {
    
}

@Scope("immortal")
class K {
    @RunsIn("a") public void foo(TestEscaping2 t) {
        J j = t.j; // disallowed because foo does not run in immortal
    }

    public void bar(TestEscaping2 t) {
        J j2 = t.j; // allowed because bar runs in immortal
    }
}