//allocfree/MethodParametersTest.java:20: Illegal allocation in a method marked @AllocFree
//        method(new String());
//               ^
//1 error

package allocfree;

import javax.safetycritical.annotate.AllocFree;

public class MethodParametersTest {
    
    @AllocFree
    public void foo() {
        method(new String());
    }
    
    @AllocFree
    public void method(String str) {
        return;
    }
 }
