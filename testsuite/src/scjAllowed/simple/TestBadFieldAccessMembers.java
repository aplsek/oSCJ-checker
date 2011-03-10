package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members=true)
public class TestBadFieldAccessMembers {
    @SCJAllowed(LEVEL_1)
    static int x;

    public void foo() {
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_FIELD_ACCESS
        int x = TestBadFieldAccessMembers.x;
    }
}
