//scjRestricted/TestOverrideInitialize3.java:23: May not override INITIALIZATION with CLEANUP or EXECUTION
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideInitialize3 {
    @SCJRestricted(Restrict.INITIALIZATION)
    public void foo() {
        
    }
}

class TestOverrideInitialize3Helper extends TestOverrideInitialize3 {
    @SCJRestricted(Restrict.EXECUTION)
    public void foo() {
        
    }
}
