package scjAllowed;

import javax.safetycritical.FakeSCJMembers;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class TestSCJMembers202 {
    /**
     * calling a non-annotated method, but FakeSCJMembers has members=true
     */
    public void foo() {
        int member = FakeSCJMembers.member;
    }
}
