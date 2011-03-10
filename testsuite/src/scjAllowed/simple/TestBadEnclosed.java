package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.LEVEL_2;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(LEVEL_2)
public class TestBadEnclosed {
    @SCJAllowed(LEVEL_1)
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_ENCLOSED
    public static int variable;

    @SCJAllowed(LEVEL_1)
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_ENCLOSED
    public static void foo() { }

    @SCJAllowed(LEVEL_1)
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_ENCLOSED
    class NestedClass {
        public void foo() { }
    }
}
