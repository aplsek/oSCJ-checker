package scjAllowed;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(value=Level.LEVEL_0, members=true)
public class TestBadNewCall {
    @SCJAllowed(Level.LEVEL_1)
    public TestBadNewCall() { }

    void foo() {
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_NEW_CALL
        new TestBadNewCall();
    }
}
