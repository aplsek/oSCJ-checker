//scope/TestRunsInClass.java:9: @RunsIn annotations must be a sub-scope of @Scope annotations.
//public class TestRunsInClass {
//       ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;

@Scope("b") @RunsIn("a")
public class TestRunsInClass {
    public void foo() {
        
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
