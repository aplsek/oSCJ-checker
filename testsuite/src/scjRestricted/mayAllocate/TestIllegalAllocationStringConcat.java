package scjRestricted.mayAllocate;

import javax.safetycritical.annotate.SCJRestricted;

public class TestIllegalAllocationStringConcat {
	@SCJRestricted(mayAllocate=false)
	public void foo() {
		String s = "s";
		//## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_ALLOCATION
		String p = "p" + s;
	}
}
