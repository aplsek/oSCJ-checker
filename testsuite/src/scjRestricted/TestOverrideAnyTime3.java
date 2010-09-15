//scjRestricted/TestOverrideAnyTime3.java:15: May not override ALL with CLEANUP, RUN, or INITIALIZATION
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideAnyTime3 {
    @SCJRestricted(Phase.ALL)
    public void foo() {
        
    }
}

class TestOverrideAnyTime3Helper extends TestOverrideAnyTime3 {
    @SCJRestricted(Phase.RUN)
    public void foo() {
        
    }
}