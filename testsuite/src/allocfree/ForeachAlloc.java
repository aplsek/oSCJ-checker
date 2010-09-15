//allocfree/ForeachAlloc.java:17: For each loops not allowed in ALLOCATE_FREE methods
//        for (int y : x) {
//        ^
//1 error

package allocfree;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class ForeachAlloc {
    @SCJRestricted(mayAllocate=false)
	public void foo(int[] x) {
		for (int y : x) {
			
		}
	}
}
