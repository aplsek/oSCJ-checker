//scope/TestRunsInClass.java:9: @RunsIn annotations must be a sub-scope of @Scope annotations.
//public class TestRunsInClass {
//       ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope("b") 
public class TestRunsInClass {
    @RunsIn("a")
    public void foo() {
        
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
                ManagedMemory.
                getCurrentManagedMemory().
                    enterPrivateMemory(0, new R2());
            }
        }
        @Scope("a") 
        @DefineScope(name="b", parent="a")
        static class R2 implements Runnable {
            @Override
            @RunsIn("b")
            public void run() {
            }
        }
    }
}
