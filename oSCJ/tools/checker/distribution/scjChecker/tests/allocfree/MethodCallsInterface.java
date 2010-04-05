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
