//scope/TestInterface2.java:20: Variables of type A are not allowed in this allocation context (immortal).
//        A a = new A();
//          ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("immortal")
interface Test2 {
    void run();
}

@Scope("immortal")
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


