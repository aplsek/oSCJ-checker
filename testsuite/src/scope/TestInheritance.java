//tests/scope/TestInheritance.java:46: (Class scope.TestInheritance has a disagreeing @Scope annotation from parent class scope.H)
//class H extends TestInheritance {
//^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope(IMMORTAL)
@RunsIn("a")
public class TestInheritance {
    public void foo() {
        
    }
    
    static class Helper {
        static void foo() {
            ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new R1());
        }
        @Scope(IMMORTAL) @RunsIn("a")
        @DefineScope(name = "a", parent = IMMORTAL)
        static class R1 implements Runnable {
            @Override
            public void run() {
                ManagedMemory.
                getCurrentManagedMemory().
                    enterPrivateMemory(0, new R2());
            }
        }
        @Scope("a") @RunsIn("b")
        @DefineScope(name = "b", parent = "a")
        static class R2 implements Runnable {
            @Override
            public void run() {
            }
        }
    }
}

@Scope("a")
@RunsIn("b")
class H extends TestInheritance {
}

@Scope(IMMORTAL)
@RunsIn("a")
class I extends TestInheritance {
    @Override
    @RunsIn("b")
    public void foo() {
    }
}