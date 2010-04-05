//allocfree/MethodCallsAbstract.java:28: Illegal allocation in a method marked @AllocFree
//        new Object();
//        ^
//1 error

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
