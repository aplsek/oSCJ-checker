//scjRestricted/TestOverrideExecution2.java:23: May not override EXECUTION with CLEANUP or INITIALIZATION
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideExecution2 {
    @SCJRestricted(Restrict.EXECUTION)
    public void foo() {
        
    }
}

class TestOverrideExecution2Helper extends TestOverrideExecution2 {
    @SCJRestricted(Restrict.CLEANUP)
    public void foo() {
        
    }
}
