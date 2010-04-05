package scjAllowed;

import javax.safetycritical.FakeSCJMembers;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * ERRORS:
 * ./tests/scjAllowed/SCJMembersTest.java:31: Illegal method call of an SCJ method.
        FakeSCJMembers.level1Call();
                                 ^
    ./tests/scjAllowed/SCJMembersTest.java:31: Illegal field access of an SCJ field.
        FakeSCJMembers.level1Call();
                      ^
    ./tests/scjAllowed/SCJMembersTest.java:32: Illegal field access of an SCJ field.
        int member = FakeSCJMembers.member;
                                   ^
    3 errors
    
 * @author plsek
 */
@SCJAllowed(members=true,value = Level.LEVEL_0)
public class SCJMembersTest {

    /**
     * calling a non-annotated method, but FakeSCJMembers has members=true
     */
    public void foo() {
        FakeSCJMembers.level1Call();
        int member = FakeSCJMembers.member;
    }
}