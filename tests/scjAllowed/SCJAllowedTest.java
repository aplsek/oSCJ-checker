//scjAllowed/SCJAllowedTest.java:31: Method call is not allowed at level 0.
//        FakeSCJ.level1Call();
//                          ^
//scjAllowed/SCJAllowedTest.java:23: @SCJAllowed(INFRASTRUCTURE) methods may not be called outside of javax.realtime or javax.safetycritical packages.
//        FakeSCJ.scjProtected();
//                            ^
//scjAllowed/SCJAllowedTest.java:34: Field access is not allowed at level 0.
//        int variable = FakeSCJ.variable;
//                              ^
//3 errors

package scjAllowed;

import javax.safetycritical.FakeSCJ;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members = true, value = Level.LEVEL_0)
public class SCJAllowedTest {
    @SCJAllowed(Level.LEVEL_0)
    public void foo() {
        FakeSCJ.level1Call();
        FakeSCJ.scjProtected();
        int variable = FakeSCJ.variable;
        
        System.out.println("tests...");
    }
}
