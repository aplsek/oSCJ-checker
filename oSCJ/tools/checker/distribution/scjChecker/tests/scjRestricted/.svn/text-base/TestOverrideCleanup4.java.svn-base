//scjRestricted/TestOverrideCleanup4.java:15: May not override CLEANUP with INITIALIZATION or EXECUTION
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideCleanup4 {
    @SCJRestricted(Restrict.CLEANUP)
    public void foo() {
        
    }
}

class TestOverrideCleanup4Helper extends TestOverrideCleanup4 {
    @SCJRestricted(Restrict.INITIALIZATION)
    public void foo() {
        
    }
}
