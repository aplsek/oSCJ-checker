//scjRestricted/TestOverrideCleanup3.java:15: May not override CLEANUP with INITIALIZATION or RUN
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideCleanup3 {
    @SCJRestricted(Phase.CLEANUP)
    public void foo() {
        
    }
}

class TestOverrideCleanup3Helper extends TestOverrideCleanup3 {
    @SCJRestricted(Phase.RUN)
    public void foo() {
        
    }
}
