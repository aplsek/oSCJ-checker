//scope/TestRunnableRunsInOverride.java:14: @RunsIn annotations must agree with their overridden annotations.
//    public void run() {
//                ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("a")
@RunsIn("a")
public class TestRunnableRunsInOverride implements Runnable {
    @RunsIn("b")
    public void run() {
        
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
                ManagedMemory.
                getCurrentManagedMemory().
                    enterPrivateMemory(0, new /*@DefineScope(name="b", parent="a")*/ R2());
            }
        }
        @Scope("a") @RunsIn("b")
        static class R2 implements Runnable {
            @Override
            public void run() {
            }
        }
    }
}
