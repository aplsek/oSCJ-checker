//scjAllowed/TestGuard501.java:18: Method call is not allowed at level 0.
//        FakeSCJ.level1Call();
//                          ^
//1 error

package scjAllowed;

import javax.safetycritical.FakeSCJ;
import javax.safetycritical.Safelet;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(value = Level.LEVEL_0, members = true)
public class TestGuard501 {

    public void Foo() {

        if (Safelet.getDeploymentLevel() == 1) {
            // we are in the guard!
            FakeSCJ.level1Call();
        }

        FakeSCJ.level1Call();
    }

}
