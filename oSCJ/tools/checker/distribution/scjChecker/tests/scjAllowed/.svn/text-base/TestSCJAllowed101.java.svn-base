//scjAllowed/TestSCJAllowed101.java:20: Method call is not allowed at level 0.
//        FakeSCJ.level1Call();
//                          ^
//1 error

package scjAllowed;

import javax.safetycritical.FakeSCJ;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members = true, value = Level.LEVEL_0)
public class TestSCJAllowed101 {
    @SCJAllowed(Level.LEVEL_0)
    public void foo() {
        FakeSCJ.level1Call();
    }
}
