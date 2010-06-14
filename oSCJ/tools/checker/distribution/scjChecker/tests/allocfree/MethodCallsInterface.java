//allocfree/MethodCallsInterface.java:24: Illegal allocation in a method marked ALLOCATE_FREE
//        new Object();
//        ^
//1 error

package allocfree;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

interface IAllocFree {
    @SCJRestricted({Restrict.ALLOCATE_FREE})
	public void foo();
}

public class MethodCallsInterface implements IAllocFree {
	public void foo() {
		new Object();
	}
}
