//allocfree/MethodParametersTest.java:20: Illegal allocation in a method marked ALLOCATE_FREE
//        method(new String());
//               ^
//1 error

package allocfree;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class MethodParametersTest {
    
    @SCJRestricted(mayAllocate=false)
    public void foo() {
        method(new String());
    }
    
    @SCJRestricted(mayAllocate=false)
    public void method(String str) {
        return;
    }
 }
