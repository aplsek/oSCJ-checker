package scjRestricted.mayAllocate;

import javax.safetycritical.annotate.SCJRestricted;

public class TestIllegalNoOverrideInherit {
    @SCJRestricted(mayAllocate=false)
	public void foo() { }

    class X extends TestIllegalNoOverrideInherit {
        @Override
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_NO_OVERRIDE
        public void foo() { }
    }
}