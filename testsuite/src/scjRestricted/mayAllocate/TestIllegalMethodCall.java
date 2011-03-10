package scjRestricted.mayAllocate;

import javax.safetycritical.annotate.SCJRestricted;

public class TestIllegalMethodCall {
    public void foo() {
        int x = bar();
    }

    @SCJRestricted(mayAllocate=false)
    public int bar() {
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_METHOD_CALL
        return baz();
    }

    public int baz() {
        return 1;
    }
}
