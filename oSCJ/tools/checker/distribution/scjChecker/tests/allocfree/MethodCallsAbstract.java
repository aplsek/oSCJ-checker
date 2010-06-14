//allocfree/MethodCallsAbstract.java:28: Illegal allocation in a method marked ALLOCATE_FREE
//        new Object();
//        ^
//1 error

package allocfree;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public abstract class MethodCallsAbstract {
    @SCJRestricted({Restrict.ALLOCATE_FREE})
	public void foo() {
		foo2();
	}
	
    @SCJRestricted({Restrict.ALLOCATE_FREE})
	public abstract void foo2();
}

class MethodCallsConcrete extends MethodCallsAbstract {
	public void foo2() {
		new Object();
	}
}
