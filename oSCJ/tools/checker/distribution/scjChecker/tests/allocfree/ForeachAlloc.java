package allocfree;

import javax.safetycritical.annotate.AllocFree;


public class ForeachAlloc {
	@AllocFree
	public void foo(int[] x) {
		for (int y : x) {
			
		}
	}
}
