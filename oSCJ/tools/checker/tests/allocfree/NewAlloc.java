//allocfree/NewAlloc.java:21: Illegal allocation in a method marked @AllocFree
//        int[] x = new int[3];
//                  ^
//allocfree/NewAlloc.java:22: Illegal allocation in a method marked @AllocFree
//        Object y = new Object();

package allocfree;

import javax.safetycritical.annotate.AllocFree;

public class NewAlloc {
	@AllocFree
	public void foo() {
		int[] x = new int[3];
		Object y = new Object();
	}
}
