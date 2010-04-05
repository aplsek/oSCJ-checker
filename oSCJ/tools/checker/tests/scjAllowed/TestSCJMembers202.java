package scjAllowed;

import javax.safetycritical.FakeSCJMembers;

public class TestSCJMembers202 {
    /**
     * calling a non-annotated method, but FakeSCJMembers has members=true
     */
    public void foo() {
        int member = FakeSCJMembers.member;
    }
}
