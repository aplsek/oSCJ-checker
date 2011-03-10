package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members=true)
public class TestBadMethodCallMembers {
    public void foo() {
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_METHOD_CALL
        bar();
    }

    @SCJAllowed(LEVEL_1)
    void bar() { }
}
