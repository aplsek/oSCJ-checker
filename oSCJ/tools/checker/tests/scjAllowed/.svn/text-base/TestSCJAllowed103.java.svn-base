//scjAllowed/TestSCJAllowed103.java:30: Field access is not allowed at level 0.
//        int variable = FakeSCJ.variable;
//                              ^
//1 error

package scjAllowed;

import javax.safetycritical.FakeSCJ;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members = true, value = Level.LEVEL_0)
public class TestSCJAllowed103 {

    @SCJAllowed(Level.LEVEL_0)
    public void foo() {
        int variable = FakeSCJ.variable;
    }
}
