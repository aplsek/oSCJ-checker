package scjAllowed;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members=true, value=Level.LEVEL_0)
public class TestBadFieldAccess {
    @SCJAllowed(Level.LEVEL_1)
    static int x;

    @SCJAllowed(Level.LEVEL_0)
    public void foo() {
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_FIELD_ACCESS
        int x = TestBadFieldAccess.x;
    }
}
