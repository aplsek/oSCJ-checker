//allocfree/AutoboxAlloc.java:23: Illegal allocation in a method marked @AllocFree
//        Integer x = 1;
//                    ^
//allocfree/AutoboxAlloc.java:24: Illegal allocation in a method marked @AllocFree
//        Integer y = x = 2;
//                      ^
//allocfree/AutoboxAlloc.java:24: Illegal allocation in a method marked @AllocFree
//        Integer y = x = 2;
//                        ^
//3 errors

package allocfree;

import javax.safetycritical.annotate.AllocFree;

public class AutoboxAlloc {
	@AllocFree
	public void foo() {
		Integer x = 1;
		Integer y = x = 2;
	}
}
