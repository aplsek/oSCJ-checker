package scjRestricted.maySelfSuspend;

import javax.safetycritical.annotate.SCJRestricted;

public class TestIllegalMethodCall {
	public void foo() {
	    //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_METHOD_CALL
		foo2();
	}

	@SCJRestricted(maySelfSuspend=true)
	public void foo2() {
	    foo2();
		foo3();
	}
	public void foo3() { }
}
