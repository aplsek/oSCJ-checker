//scope/TestVariableScope.java:12: Variables of type D are not allowed in this allocation context (immortal).
//    public void foo(D d) {
//                      ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("immortal")
public class TestVariableScope {
    public void foo(D d) {
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

@Scope("a")
class D {
    
}
