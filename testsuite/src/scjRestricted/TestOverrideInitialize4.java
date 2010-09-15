package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideInitialize4 {     // FAIL
    @SCJRestricted(Phase.INITIALIZATION)
    public void foo() {
        
    }
}

class TestOverrideInitialize4Helper extends TestOverrideInitialize4 {      // FAIL
    @SCJRestricted(Phase.INITIALIZATION)
    public void foo() {
        
    }
}
