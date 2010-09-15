//scjRestricted/TestOverrideExecution2.java:23: May not override RUN with CLEANUP or INITIALIZATION
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideExecution2 {
    @SCJRestricted(Phase.RUN)
    public void foo() {
        
    }
}

class TestOverrideExecution2Helper extends TestOverrideExecution2 {
    @SCJRestricted(Phase.CLEANUP)
    public void foo() {
        
    }
}
