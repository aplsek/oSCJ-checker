//allocfree/StringAlloc.java:19: Illegal allocation in a method marked @AllocFree
//        String p = "p" + s;
//                       ^
//1 error

package allocfree;

import javax.safetycritical.annotate.AllocFree;

public class StringAlloc {
	@AllocFree
	public void foo() {
		String s = "s";
		String p = "p" + s;
	}
}
