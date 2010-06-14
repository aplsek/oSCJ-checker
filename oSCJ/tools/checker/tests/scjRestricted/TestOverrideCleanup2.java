package scjRestricted;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideCleanup2 {
    @SCJRestricted(Restrict.CLEANUP)
    public void foo() {
        
    }
}

class TestOverrideCleanup2Helper extends TestOverrideCleanup2 {
    @SCJRestricted(Restrict.CLEANUP)
    public void foo() {
        
    }
}
