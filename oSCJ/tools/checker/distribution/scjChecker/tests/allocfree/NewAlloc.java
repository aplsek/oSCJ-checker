//allocfree/NewAlloc.java:21: Illegal allocation in a method marked ALLOCATE_FREE
//        int[] x = new int[3];
//                  ^
//allocfree/NewAlloc.java:22: Illegal allocation in a method marked ALLOCATE_FREE
//        Object y = new Object();

package allocfree;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class NewAlloc {
    @SCJRestricted({Restrict.ALLOCATE_FREE})
	public void foo() {
		int[] x = new int[3];
		Object y = new Object();
	}
}
