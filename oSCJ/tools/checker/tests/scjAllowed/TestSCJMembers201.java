package scjAllowed;

import javax.safetycritical.FakeSCJMembers;

/**
 * ERRORS:
 * ./tests/scjAllowed/SCJMembersTest.java:31: Illegal method call of an SCJ method.
        FakeSCJMembers.level1Call();
                                 ^
    ./tests/scjAllowed/SCJMembersTest.java:31: Illegal field access of an SCJ field.
        FakeSCJMembers.level1Call();
                      ^
    2 errors
    
 * @author plsek
 */
public class TestSCJMembers201 {
    /**
     * calling a non-annotated method, but FakeSCJMembers has members=true
     */
    public void foo() {
        FakeSCJMembers.level1Call();
    }
}
