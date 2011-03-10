package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.LEVEL_0;
import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members=true, value=LEVEL_0)
public class TestBadFieldAccess {
    @SCJAllowed(LEVEL_1)
    static int x;

    @SCJAllowed(LEVEL_0)
    public void foo() {
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_FIELD_ACCESS
        int x = TestBadFieldAccess.x;
    }
}
