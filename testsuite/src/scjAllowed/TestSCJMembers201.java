///Users/plsek/_work/workspace_RT/scj-annotations/tests/scjAllowed/TestSCJMembers201.java:22: Hidden code can not invoke an SCJAllowed code.
//        FakeSCJMembers.level1Call();
//                                 ^
///Users/plsek/_work/workspace_RT/scj-annotations/tests/scjAllowed/TestSCJMembers201.java:26: Methods outside of javax.realtime or javax.safetycritical packages cannot be annotated with @SCJAllowed(INFRASTRUCTURE).
//    public void badInfrastructure () {
//                ^
//2 errors

package scjAllowed;

import javax.safetycritical.FakeSCJMembers;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;


@SCJAllowed
public class TestSCJMembers201 {
    /**
     * calling a non-annotated method, but FakeSCJMembers has members=true
     */
    public void foo() {
        FakeSCJMembers.level1Call();
    }
    
    @SCJAllowed(INFRASTRUCTURE)
    public void badInfrastructure () {
    }
}
