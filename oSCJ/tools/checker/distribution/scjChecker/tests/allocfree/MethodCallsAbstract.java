package allocfree;

import javax.safetycritical.annotate.AllocFree;


public abstract class MethodCallsAbstract {
	@AllocFree
	public void foo() {
		foo2();
	}
	
	@AllocFree
	public abstract void foo2();
}

class MethodCallsConcrete extends MethodCallsAbstract {
	public void foo2() {
		new Object();
	}
}
