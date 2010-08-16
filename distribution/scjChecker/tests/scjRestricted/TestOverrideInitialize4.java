package scjRestricted;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideInitialize4 {     // FAIL
    @SCJRestricted(Restrict.INITIALIZATION)
    public void foo() {
        
    }
}

class TestOverrideInitialize4Helper extends TestOverrideInitialize4 {      // FAIL
    @SCJRestricted(Restrict.INITIALIZATION)
    public void foo() {
        
    }
}
