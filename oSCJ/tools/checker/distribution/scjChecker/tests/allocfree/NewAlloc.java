package allocfree;

import javax.safetycritical.annotate.AllocFree;


public class NewAlloc {
	@AllocFree
	public void foo() {
		int[] x = new int[3];
		Object y = new Object();
	}
}
