//scope/TestInvoke.java:20: Illegal invocation of method of object in scope a while in scope immortal.
//        foo();
//           ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope("immortal")
public class TestInvoke {
    @RunsIn("a")
    public void foo() {
        baz();
    }
    
    public void bar() {
        foo();
    }
    
    @SCJRestricted(mayAllocate=false)
    public void baz() {
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
