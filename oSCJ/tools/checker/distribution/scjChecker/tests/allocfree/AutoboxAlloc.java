package allocfree;

import javax.safetycritical.annotate.AllocFree;


public class AutoboxAlloc {
	@AllocFree
	public void foo() {
		Integer x = 1;
		Integer y = x = 2;
	}
}
