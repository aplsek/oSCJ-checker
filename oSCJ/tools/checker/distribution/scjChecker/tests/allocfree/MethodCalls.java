package allocfree;

import javax.safetycritical.annotate.AllocFree;


public class MethodCalls {
	public void foo() {
		int x = foo2(); // legal
	}
	
	@AllocFree
	public int foo2() {
		return foo3(); // illegal
	}
	
	public int foo3() {
		return 1;
	}
}