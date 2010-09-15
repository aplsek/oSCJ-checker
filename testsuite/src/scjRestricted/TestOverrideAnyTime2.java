//scjRestricted/TestOverrideAnyTime2.java:15: May not override ALL with CLEANUP, RUN, or INITIALIZATION
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideAnyTime2 {
    @SCJRestricted(Phase.ALL)
    public void foo() {
        
    }
}

class TestOverrideAnyTime2Helper extends TestOverrideAnyTime2 {
    @SCJRestricted(Phase.CLEANUP)
    public void foo() {
        
    }
}