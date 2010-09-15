//scope/TestRunsIn.java:16: Methods must run in the same scope or a child scope of their owning type.
//    public void foo() {           // ERROR  - a is parent of b
//                ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("b")
public class TestRunsIn {
    @RunsIn("a")
    public void foo() {          // ERROR  - a is parent of b
        
    }
    static class Helper {
        static void foo() {     
            ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new /*@DefineScope(name="a", parent="immortal")*/ R1());
        }
    }
}

@Scope("immortal") @RunsIn("a")
class R1 implements Runnable {
    @Override
    public void run() {
        
        @DefineScope(name="b", parent="a")
        R2 r2 = new R2();
        
        ManagedMemory.
        getCurrentManagedMemory().
            enterPrivateMemory(0, r2);
        
        //ManagedMemory.
        //getCurrentManagedMemory().
        //    enterPrivateMemory(0, new /*@DefineScope(name="b", parent="a")*/ R2());
    }
}

@Scope("a") 
@RunsIn("b")
class R2 implements Runnable {
    @Override
    public void run() {
    }
}