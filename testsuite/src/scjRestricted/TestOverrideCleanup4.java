//scjRestricted/TestOverrideCleanup4.java:15: May not override CLEANUP with INITIALIZATION or RUN
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideCleanup4 {
    @SCJRestricted(Phase.CLEANUP)
    public void foo() {
        
    }
}

class TestOverrideCleanup4Helper extends TestOverrideCleanup4 {
    @SCJRestricted(Phase.INITIALIZATION)
    public void foo() {
        
    }
}
