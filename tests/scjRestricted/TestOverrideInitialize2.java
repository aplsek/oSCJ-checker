//scjRestricted/TestOverrideInitialize2.java:23: May not override INITIALIZATION with CLEANUP or EXECUTION
//    public void foo() {
//                ^
//1 error

package scjRestricted;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideInitialize2 {
    @SCJRestricted(Restrict.INITIALIZATION)
    public void foo() {
        
    }
}

class TestOverrideInitialize2Helper extends TestOverrideInitialize2 {
    @SCJRestricted(Restrict.CLEANUP)
    public void foo() {
        
    }
}
