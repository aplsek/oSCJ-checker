//tests/defineScope/TestDuplicate.java:21: Duplicate scope name from @DefineScope.
//                enterPrivateMemory(0, new /*@DefineScope(name="a", parent=IMMORTAL)*/ R1());
//                                  ^
//1 error

package defineScope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

public class TestDuplicate {

    @DefineScope(name="a", parent=IMMORTAL)
    PrivateMemory a;
    
    static class Helper {
        static void foo() {
            ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new /*@DefineScope(name="a", parent=IMMORTAL)*/ R1());
        }
        
        @DefineScope(name="a", parent=IMMORTAL)
        @Scope(IMMORTAL) @RunsIn("a")
        static class R1 implements Runnable {
            @Override
            public void run() {
            }
        }
    }
    
    
}
