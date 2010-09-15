//./tests/allocfree/MethodCallsInterface.java:17: The inherited/overridden annotations must be restate. The overriding method is missing the @SCJRestricted annotation while the overriden method has one.
//    public void foo() {
//                ^
//1 error

package allocfree;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

interface IAllocFree {
    @SCJRestricted(mayAllocate=false)
	public void foo();
}

public class MethodCallsInterface implements IAllocFree {
	public void foo() {                                                // ERROR
		new Object();
	}
}
