package scjRestricted.mayAllocate;

import javax.safetycritical.annotate.SCJRestricted;

public class TestIllegalForeach {
    @SCJRestricted(mayAllocate=false)
    public void foo(int[] x) {
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_FOREACH
        for (int y : x) { }
    }
}
