package scjRestricted.phase;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestIllegalMethodCall2 {
    @SCJRestricted(Phase.INITIALIZATION)
    public void baz() {
        MyFoo foo = new MyFoo();
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_METHOD_CALL
        foo.init(); // TODO: init is no INITIALIZATION, is this legal??
    }
}

class MyFoo {
    public void init() {
    }
}