//scope/TestUpcast.java:50: Cannot assign expression in scope a to variable in scope immortal.
//        TestUpcast.cast = cast;   // ERROR
//                        ^
//scope/TestUpcast.java:52: Cannot assign expression in scope null to variable in scope immortal.
//        TestUpcast.upCast = (TestUpcast) cast;  // ERROR
//                          ^
///Users/plsek/_work/workspace_RT/scj-annotations/tests/scope/TestUpcast.java:54: Cannot assign expression in scope a to variable in scope immortal.
//        TestUpcast.upCast = cast;   // ERROR
//                          ^
//3 errors

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

public class TestUpcast {
   
    static public L cast = null;
    static public TestUpcast upCast = null;
    
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

@Scope("a")
class L extends TestUpcast {
    public void foo() {
        TestUpcast f = (TestUpcast) new L(); // OK
        TestUpcast g = new L(); // OK
        
        L cast = new L();
        TestUpcast fail = (TestUpcast) cast; // OK
        
        TestUpcast.cast = cast;   // ERROR
        
        TestUpcast.upCast = (TestUpcast) cast;  // ERROR
        
        TestUpcast.upCast = cast;   // ERROR
    }
}