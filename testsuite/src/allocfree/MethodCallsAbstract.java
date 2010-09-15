//./tests/allocfree/MethodCallsAbstract.java:23: The inherited/overridden annotations must be restate. The overriding method is missing the @SCJRestricted annotation while the overriden method has one.
//    public void foo2() {
//                ^
//1 error

package allocfree;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public abstract class MethodCallsAbstract {
    @SCJRestricted(mayAllocate=false)
	public void foo() {
		foo2();
	}
	
    @SCJRestricted(mayAllocate=false)
	public abstract void foo2();
}

class MethodCallsConcrete extends MethodCallsAbstract {
	
    public void foo2() {
		new Object();                 // ERROR
	}
}
