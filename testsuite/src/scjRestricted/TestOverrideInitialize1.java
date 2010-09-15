package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideInitialize1 {
    @SCJRestricted(Phase.INITIALIZATION)
    public void foo() {
        
    }
}

class TestOverrideInitialize1Helper extends TestOverrideInitialize1 {
    @SCJRestricted(Phase.ALL)
    public void foo() {
        
    }
}
