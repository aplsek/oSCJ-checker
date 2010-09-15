//scjRestricted/TestOverrideExecution4.java:23: May not override RUN with CLEANUP or INITIALIZATION
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideExecution4 {
    @SCJRestricted(Phase.RUN)
    public void foo() {
        
    }
}

class TestOverrideExecution4Helper extends TestOverrideExecution4 {
    @SCJRestricted(Phase.INITIALIZATION)
    public void foo() {
        
    }
}
