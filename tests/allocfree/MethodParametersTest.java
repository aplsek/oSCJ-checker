//allocfree/MethodParametersTest.java:20: Illegal allocation in a method marked ALLOCATE_FREE
//        method(new String());
//               ^
//1 error

package allocfree;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class MethodParametersTest {
    
    @SCJRestricted({Restrict.ALLOCATE_FREE})
    public void foo() {
        method(new String());
    }
    
    @SCJRestricted({Restrict.ALLOCATE_FREE})
    public void method(String str) {
        return;
    }
 }
