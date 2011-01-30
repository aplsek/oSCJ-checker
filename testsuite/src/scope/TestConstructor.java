//scope/TestConstructor.java:14: @RunsIn annotations not allowed on constructors.
//    public TestConstructor() {
//           ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope(IMMORTAL)
public class TestConstructor {
   
    @RunsIn("a")
    public TestConstructor() {
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
