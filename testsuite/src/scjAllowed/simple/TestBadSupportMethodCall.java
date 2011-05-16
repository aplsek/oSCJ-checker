package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.LEVEL_0;
import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(value=LEVEL_0)
public class TestBadSupportMethodCall {
    @SCJAllowed(SUPPORT)
    public void foo() {
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_SUPPORT_METHOD_CALL
        bar();
    }
    @SCJAllowed(SUPPORT)
    public void bar() { }
}
