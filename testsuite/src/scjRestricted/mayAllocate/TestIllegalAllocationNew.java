package scjRestricted.mayAllocate;

import javax.safetycritical.annotate.SCJRestricted;

public class TestIllegalAllocationNew {
    @SCJRestricted(mayAllocate=false)
	public void foo() {
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_ALLOCATION
		int[] x = new int[3];
		//## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_ALLOCATION
		Object y = new Object();
	}
}
