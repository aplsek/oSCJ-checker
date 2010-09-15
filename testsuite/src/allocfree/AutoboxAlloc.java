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

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class AutoboxAlloc {
    @SCJRestricted(mayAllocate=false)
	public void foo() {
		Integer x = 1;
		Integer y = x = 2;
	}
}
