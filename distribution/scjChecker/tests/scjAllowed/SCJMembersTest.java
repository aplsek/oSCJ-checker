//scjAllowed/SCJMembersTest.java:29: Method call is not allowed at level 0.
//        FakeSCJMembers.level1Call();
//                                 ^
//scjAllowed/SCJMembersTest.java:30: Field access is not allowed at level 0.
//        int member = FakeSCJMembers.member;
//                                   ^
//2 errors

package scjAllowed;

import javax.safetycritical.FakeSCJMembers;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members=true)
public class SCJMembersTest {

    /**
     * calling a non-annotated method, but FakeSCJMembers has members=true
     */
    public void foo() {
        FakeSCJMembers.level1Call();
        int member = FakeSCJMembers.member;
    }
}