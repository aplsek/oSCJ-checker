package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.LEVEL_0;
import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(value=LEVEL_0, members=true)
public class TestBadNewCall {
    @SCJAllowed(LEVEL_1)
    public TestBadNewCall() { }

    void foo() {
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_NEW_CALL
        new TestBadNewCall();
    }
}
