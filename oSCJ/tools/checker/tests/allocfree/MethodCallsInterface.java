//allocfree/MethodCallsInterface.java:24: Illegal allocation in a method marked @AllocFree
//        new Object();
//        ^
//1 error

package allocfree;

import javax.safetycritical.annotate.AllocFree;

interface IAllocFree {
	@AllocFree
	public void foo();
}

public class MethodCallsInterface implements IAllocFree {
	public void foo() {
		new Object();
	}
}
