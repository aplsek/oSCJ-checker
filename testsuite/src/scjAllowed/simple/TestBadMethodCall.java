package scjAllowed.simple;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(value=Level.LEVEL_0)
public class TestBadMethodCall {
    @SCJAllowed(Level.LEVEL_0)
    public void foo() {
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_METHOD_CALL
        bar();
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_METHOD_CALL
        baz();
    }
    @SCJAllowed(Level.LEVEL_1)
    public void bar() { }

    static void baz() { }
}
