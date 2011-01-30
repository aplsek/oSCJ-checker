//scope/TestInterface2.java:20: Variables of type A are not allowed in this allocation context (IMMORTAL).
//        A a = new A();
//          ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope(IMMORTAL)
interface Test2 {
    void run();
}

@Scope(IMMORTAL)
public class TestInterface2 implements Test2 {
    @Scope("a")
    class A {
        
    }
    public void run() {
        A a = new A();
    }
    
    static class Helper {
        static void foo() {
            ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new R1());
        }
        @Scope(IMMORTAL) 
        @DefineScope(name = "a", parent = IMMORTAL)
        static class R1 implements Runnable {
            @Override
            @RunsIn("a")
            public void run() {
            }
        }
    }
}


