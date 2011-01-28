//scope/TestRunnableRunsInOverride.java:14: @RunsIn annotations must agree with their overridden annotations.
//    public void run() {
//                ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope("a")
@RunsIn("a")
@DefineScope(name = "a", parent = IMMORTAL)
public class TestRunnableRunsInOverride implements Runnable {
    @RunsIn("b")
    public void run() {
        
    }
    
    static class Helper {
        static void foo() {
            ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new R1());
        }
        @Scope(IMMORTAL) @RunsIn("a")
        static class R1 implements Runnable {
            @Override
            public void run() {
                ManagedMemory.
                getCurrentManagedMemory().
                    enterPrivateMemory(0, new R2());
            }
        }
        @Scope("a") @RunsIn("b")
        @DefineScope(name = "b", parent = IMMORTAL)
        static class R2 implements Runnable {
            @Override
            public void run() {
            }
        }
    }
}
