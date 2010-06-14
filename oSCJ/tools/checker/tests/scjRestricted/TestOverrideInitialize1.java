package scjRestricted;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideInitialize1 {
    @SCJRestricted(Restrict.INITIALIZATION)
    public void foo() {
        
    }
}

class TestOverrideInitialize1Helper extends TestOverrideInitialize1 {
    @SCJRestricted(Restrict.ANY_TIME)
    public void foo() {
        
    }
}
