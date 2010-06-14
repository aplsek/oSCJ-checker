//scjRestricted/TestOverrideAnyTime4.java:15: May not override ANY_TIME with CLEANUP, EXECUTION, or INITIALIZATION
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideAnyTime4 {
    @SCJRestricted(Restrict.ANY_TIME)
    public void foo() {
        
    }
}

class TestOverrideAnyTime4Helper extends TestOverrideAnyTime4 {
    @SCJRestricted(Restrict.INITIALIZATION)
    public void foo() {
        
    }
}