//allocfree/ForeachAlloc.java:17: For each loops not allowed in ALLOCATE_FREE methods
//        for (int y : x) {
//        ^
//1 error

package allocfree;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class ForeachAlloc {
    @SCJRestricted({Restrict.ALLOCATE_FREE})
	public void foo(int[] x) {
		for (int y : x) {
			
		}
	}
}
