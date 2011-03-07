package scjRestricted.mayAllocate;

import javax.safetycritical.annotate.SCJRestricted;

public class TestIllegalAllocationAutobox {
    @SCJRestricted(mayAllocate=false)
    public void foo(Integer z) {
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_ALLOCATION
        Integer x = 1;
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_ALLOCATION
        Integer y = x = 2;
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_ALLOCATION
        z += 2;
    }
}
