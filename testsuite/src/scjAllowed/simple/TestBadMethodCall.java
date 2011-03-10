package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.LEVEL_0;
import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(value=LEVEL_0)
public class TestBadMethodCall {
    @SCJAllowed(LEVEL_0)
    public void foo() {
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_METHOD_CALL
        bar();
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_METHOD_CALL
        baz();
    }
    @SCJAllowed(LEVEL_1)
    public void bar() { }

    static void baz() { }
}
