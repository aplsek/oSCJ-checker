//scjRestricted/TestOverrideExecution4.java:23: May not override EXECUTION with CLEANUP or INITIALIZATION
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideExecution4 {
    @SCJRestricted(Restrict.EXECUTION)
    public void foo() {
        
    }
}

class TestOverrideExecution4Helper extends TestOverrideExecution4 {
    @SCJRestricted(Restrict.INITIALIZATION)
    public void foo() {
        
    }
}
