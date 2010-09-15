//allocfree/StringAlloc.java:19: Illegal allocation in a method marked ALLOCATE_FREE
//        String p = "p" + s;
//                       ^
//1 error

package allocfree;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class StringAlloc {
	@SCJRestricted(mayAllocate=false)
	public void foo() {
		String s = "s";
		String p = "p" + s;
	}
}
