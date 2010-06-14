//scjRestricted/TestOverrideCleanup3.java:15: May not override CLEANUP with INITIALIZATION or EXECUTION
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideCleanup3 {
    @SCJRestricted(Restrict.CLEANUP)
    public void foo() {
        
    }
}

class TestOverrideCleanup3Helper extends TestOverrideCleanup3 {
    @SCJRestricted(Restrict.EXECUTION)
    public void foo() {
        
    }
}
