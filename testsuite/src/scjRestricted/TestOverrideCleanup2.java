package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideCleanup2 {
    @SCJRestricted(Phase.CLEANUP)
    public void foo() {
        
    }
}

class TestOverrideCleanup2Helper extends TestOverrideCleanup2 {
    @SCJRestricted(Phase.CLEANUP)
    public void foo() {
        
    }
}
