//scjRestricted/TestOverrideAnyTime4.java:15: May not override ALL with CLEANUP, RUN, or INITIALIZATION
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideAnyTime4 {
    @SCJRestricted(Phase.ALL)
    public void foo() {
        
    }
}

class TestOverrideAnyTime4Helper extends TestOverrideAnyTime4 {
    @SCJRestricted(Phase.INITIALIZATION)
    public void foo() {
        
    }
}