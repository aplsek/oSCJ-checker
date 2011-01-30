//scope/TestVariableScope.java:12: Variables of type D are not allowed in this allocation context (IMMORTAL).
//    public void foo(D d) {
//                      ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;


@Scope(IMMORTAL)
public class TestVariableScope {
    public void foo(D d) {
    }
    
    static class Helper {
        static void foo() {
            ManagedMemory.
                getCurrentManagedMemory().
                    enterPrivateMemory(0, new R1());
        }
        @Scope(IMMORTAL)
        @DefineScope(name="a",parent=IMMORTAL)
        static class R1 implements Runnable {
            @Override
            @RunsIn("a")
            public void run() {
            }
        }
    }
}

@Scope("a")
class D {
}
