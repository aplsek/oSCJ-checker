//allocfree/ForeachAlloc.java:17: For each loops not allowed in @AllocFree methods
//        for (int y : x) {
//        ^
//1 error

package allocfree;

import javax.safetycritical.annotate.AllocFree;

public class ForeachAlloc {
	@AllocFree
	public void foo(int[] x) {
		for (int y : x) {
			
		}
	}
}
