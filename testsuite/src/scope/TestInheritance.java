//tests/scope/TestInheritance.java:46: (Class scope.TestInheritance has a disagreeing @Scope annotation from parent class scope.H)
//class H extends TestInheritance {
//^
//scope/TestInheritance.java:61: @RunsIn annotations must agree with their overridden annotations.
//    public void foo() {
//                ^
//2 errors

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope(IMMORTAL)
public class TestInheritance {
   
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
        @DefineScope(name = "a", parent = IMMORTAL)
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
        @DefineScope(name = "b", parent = "a")
        static class R2 implements Runnable {
            @Override
            @RunsIn("b")
            public void run() {
            }
        }
    }
}

@Scope("a")
class H extends TestInheritance {
    @RunsIn("b")
    public void foo() {}

}

@Scope(IMMORTAL)
class I extends TestInheritance {
    @Override
    @RunsIn("b")
    public void foo() {
    }
}