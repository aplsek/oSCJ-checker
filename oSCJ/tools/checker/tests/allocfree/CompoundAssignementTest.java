//allocfree/CompoundAssignementTest.java:22: Illegal allocation in a method marked @AllocFree
//        i += 2;
//             ^
//1 error

package allocfree;

import javax.safetycritical.annotate.AllocFree;

public class CompoundAssignementTest {

    Integer i = new Integer(0);
    
    @AllocFree
    public  void foo() {
        i += 2;
    }
}
