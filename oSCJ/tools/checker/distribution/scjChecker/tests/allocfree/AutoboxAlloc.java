//allocfree/AutoboxAlloc.java:23: Illegal allocation in a method marked ALLOCATE_FREE
//        Integer x = 1;
//                    ^
//allocfree/AutoboxAlloc.java:24: Illegal allocation in a method marked ALLOCATE_FREE
//        Integer y = x = 2;
//                      ^
//allocfree/AutoboxAlloc.java:24: Illegal allocation in a method marked ALLOCATE_FREE
//        Integer y = x = 2;
//                        ^
//3 errors

package allocfree;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class AutoboxAlloc {
    @SCJRestricted({Restrict.ALLOCATE_FREE})
	public void foo() {
		Integer x = 1;
		Integer y = x = 2;
	}
}
