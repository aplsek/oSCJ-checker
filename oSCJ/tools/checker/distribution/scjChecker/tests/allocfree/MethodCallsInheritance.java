//allocfree/MethodCallsInheritance.java:31: Illegal invocation of a method annotated MAY_ALLOCATE from within a method annotated ALLOCATE_FREE
//        foo3();
//            ^
//1 error

package allocfree;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class MethodCallsInheritance {
    @SCJRestricted({Restrict.ALLOCATE_FREE})
	public void foo() {
		foo2();
	}
	
    @SCJRestricted({Restrict.ALLOCATE_FREE})
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