//scjRestricted/TestOverrideInitialize2.java:23: May not override INITIALIZATION with CLEANUP or RUN
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideInitialize2 {
    @SCJRestricted(Phase.INITIALIZATION)
    public void foo() {
        
    }
}

class TestOverrideInitialize2Helper extends TestOverrideInitialize2 {
    @SCJRestricted(Phase.CLEANUP)
    public void foo() {
        
    }
}
