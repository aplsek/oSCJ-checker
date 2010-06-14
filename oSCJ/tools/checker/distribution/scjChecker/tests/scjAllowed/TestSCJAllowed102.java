//scjAllowed/TestSCJAllowed102.java:17: @SCJAllowed(INFRASTRUCTURE) methods may not be called outside of javax.realtime or javax.safetycritical packages.
//        FakeSCJ.scjProtected();
//                            ^
//1 error

package scjAllowed;

import javax.safetycritical.FakeSCJ;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members = true, value = Level.LEVEL_0)
public class TestSCJAllowed102 {

    @SCJAllowed(Level.LEVEL_0)
    public void foo() {
        FakeSCJ.scjProtected();
    }
}
