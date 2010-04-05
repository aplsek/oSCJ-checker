
package allocfree;

import javax.safetycritical.annotate.AllocFree;


public class MethodCallsInheritance {
	@AllocFree
	public void foo() {
		foo2();
	}
	
	@AllocFree
	public void foo2() {
		
	}
}

class MethodCalls3 extends MethodCallsInheritance {
	public void foo() {
		foo3();
	}
	
	public void foo3() {
		
	}
}