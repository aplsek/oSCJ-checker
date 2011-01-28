//scope/TestRunsIn.java:16: Methods must run in the same scope or a child scope of their owning type.
//    public void foo() {           // ERROR  - a is parent of b
//                ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;


@Scope("b")
public class TestRunsIn {
    @RunsIn("a")
    public void foo() {          // ERROR  - a is parent of b
        
    }
    static class Helper {
        static void foo() {     
            ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new R1());
        }
    }
}

@Scope(IMMORTAL) @RunsIn("a")
@DefineScope(name = "a", parent = IMMORTAL)
class R1 implements Runnable {
    @Override
    public void run() {
        
        @DefineScope(name="b", parent="a")
        R2 r2 = new R2();
        
        ManagedMemory.
        getCurrentManagedMemory().
            enterPrivateMemory(0, r2);
        
    }
}

@Scope("a") 
@RunsIn("b")
@DefineScope(name = "b", parent = "a")
class R2 implements Runnable {
    @Override
    public void run() {
    }
}