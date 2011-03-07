package scjRestricted.mayAllocate;

import javax.safetycritical.annotate.SCJRestricted;

public abstract class TestIllegalNoOverrideAbstract {
    @SCJRestricted(mayAllocate=false)
    public abstract void foo();

    static class X extends TestIllegalNoOverrideAbstract {
        @Override
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_NO_OVERRIDE
        public void foo() { }
    }
}
