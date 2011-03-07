package scjRestricted.mayAllocate;

import javax.safetycritical.annotate.SCJRestricted;

interface TestIllegalNoOverrideInterface {
    @SCJRestricted(mayAllocate=false)
	public void foo();

    static class X implements TestIllegalNoOverrideInterface {
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_NO_OVERRIDE
        public void foo() { }
    }
}
