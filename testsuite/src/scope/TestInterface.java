//scope/TestInterface.java:27: @RunsIn annotations must agree with their overridden annotations.
//    public void run() {           // ERROR
//                ^
//1 error

package scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
interface Test {
    @RunsIn("a")
    void run();
}

@Scope(IMMORTAL)
public class TestInterface implements Test {
    @Scope("a")
    class A {
        
    }
    
    public void run() {             // ERROR
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
