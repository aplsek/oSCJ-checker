//scjAllowed/SCJMembersTest.java:29: Method call is not allowed at level 0.
//        FakeSCJMembers.level1Call();
//                                 ^
//scjAllowed/SCJMembersTest.java:30: Field access is not allowed at level 0.
//        int member = FakeSCJMembers.member;
//                                   ^
//2 errors

package scjAllowed;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members=true)
public class TestBadFieldAccessMembers {
    @SCJAllowed(Level.LEVEL_1)
    static int x;

    public void foo() {
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_FIELD_ACCESS
        int x = TestBadFieldAccessMembers.x;
    }
}