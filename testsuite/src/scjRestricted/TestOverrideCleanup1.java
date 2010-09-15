package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideCleanup1 {
    @SCJRestricted(Phase.CLEANUP)
    public void foo() {
        
    }
}

class TestOverrideCleanup1Helper extends TestOverrideCleanup1 {
    @SCJRestricted(Phase.ALL)
    public void foo() {
        
    }
}
