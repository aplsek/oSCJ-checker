//tests/defineScope/TestDuplicate.java:21: Duplicate scope name from @DefineScope.
//                enterPrivateMemory(0, new /*@DefineScope(name="a", parent="immortal")*/ R1());
//                                  ^
//1 error

package defineScope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

public class TestDuplicate {

    @DefineScope(name="a", parent="immortal")
    PrivateMemory a;
    
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
