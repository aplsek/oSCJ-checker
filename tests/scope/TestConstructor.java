//scope/TestConstructor.java:14: @RunsIn annotations not allowed on constructors.
//    public TestConstructor() {
//           ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("immortal")
public class TestConstructor {
   
    @RunsIn("a")
    public TestConstructor() {
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
