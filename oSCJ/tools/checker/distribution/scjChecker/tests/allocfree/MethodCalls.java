//allocfree/MethodCalls.java:23: Illegal invocation of a method annotated MAY_ALLOCATE from within a method annotated ALLOCATE_FREE
//        return foo3(); // illegal
//                   ^
//1 error

package allocfree;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class MethodCalls {
	public void foo() {
		int x = foo2(); // legal
	}
	
	@SCJRestricted({Restrict.ALLOCATE_FREE})
	public int foo2() {
		return foo3(); // illegal
	}
	
	public int foo3() {
		return 1;
	}
}