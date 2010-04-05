package allocfree;

import javax.safetycritical.annotate.AllocFree;


public class StringAlloc {
	@AllocFree
	public void foo() {
		String s = "s";
		String p = "p" + s;
	}
}
