package scjRestricted;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideCleanup1 {
    @SCJRestricted(Restrict.CLEANUP)
    public void foo() {
        
    }
}

class TestOverrideCleanup1Helper extends TestOverrideCleanup1 {
    @SCJRestricted(Restrict.ANY_TIME)
    public void foo() {
        
    }
}
