//allocfree/CompoundAssignementTest.java:22: Illegal allocation in a method marked ALLOCATE_FREE
//        i += 2;
//             ^
//1 error

package allocfree;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class CompoundAssignementTest {

    Integer i = new Integer(0);
    
    @SCJRestricted(mayAllocate=false)
    public  void foo() {
        i += 2;
    }
}
