//./tests/allocfree/MethodCallsInheritance.java:24: The inherited/overridden annotations must be restate. The overriding method is missing the @SCJRestricted annotation while the overriden method has one.
//    public void foo() {
//                ^
//1 error

package allocfree;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class MethodCallsInheritance {
    @SCJRestricted(mayAllocate=false)
	public void foo() {
		foo2();
	}
	
    @SCJRestricted(mayAllocate=false)
	public void foo2() {
		
	}
}

class MethodCalls3 extends MethodCallsInheritance {
	public void foo() {                                // ERROR
		foo3();
	}
	
	public void foo3() {   
		
	}
}