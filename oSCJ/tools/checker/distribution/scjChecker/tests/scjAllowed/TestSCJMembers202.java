package scjAllowed;

import javax.safetycritical.FakeSCJMembers;

/**
 * ERRORS:
 * 
    ./tests/scjAllowed/SCJMembersTest.java:32: Illegal field access of an SCJ field.
        int member = FakeSCJMembers.member;
                                   ^
    1 error
    
 * @author plsek
 */
public class TestSCJMembers202 {
    /**
     * calling a non-annotated method, but FakeSCJMembers has members=true
     */
    public void foo() {
        int member = FakeSCJMembers.member;
    }
}
