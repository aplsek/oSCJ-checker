//scjRestricted/TestOverrideInitialize3.java:23: May not override INITIALIZATION with CLEANUP or RUN
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideInitialize3 {
    @SCJRestricted(Phase.INITIALIZATION)
    public void foo() {
        
    }
}

class TestOverrideInitialize3Helper extends TestOverrideInitialize3 {
    @SCJRestricted(Phase.RUN)
    public void foo() {
        
    }
}
