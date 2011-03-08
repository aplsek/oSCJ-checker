package scjAllowed;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members=true)
public class TestBadMethodCallMembers {

    public void foo() {
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_METHOD_CALL
        bar();
    }

    @SCJAllowed(Level.LEVEL_1)
    void bar() { }
}