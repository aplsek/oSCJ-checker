package scjAllowed;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(Level.LEVEL_2)
public class TestBadEnclosed {
    @SCJAllowed(Level.LEVEL_1)
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_ENCLOSED
    public static int variable;

    @SCJAllowed(Level.LEVEL_1)
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_ENCLOSED
    public static void foo() { }

    @SCJAllowed(Level.LEVEL_1)
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_ENCLOSED
    class NestedClass {
        public void foo() { }
    }
}