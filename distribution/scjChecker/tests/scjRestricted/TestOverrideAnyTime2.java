//scjRestricted/TestOverrideAnyTime2.java:15: May not override ANY_TIME with CLEANUP, EXECUTION, or INITIALIZATION
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideAnyTime2 {
    @SCJRestricted(Restrict.ANY_TIME)
    public void foo() {
        
    }
}

class TestOverrideAnyTime2Helper extends TestOverrideAnyTime2 {
    @SCJRestricted(Restrict.CLEANUP)
    public void foo() {
        
    }
}