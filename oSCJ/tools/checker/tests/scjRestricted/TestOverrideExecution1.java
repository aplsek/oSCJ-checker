package scjRestricted;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideExecution1 {
    @SCJRestricted(Restrict.EXECUTION)
    public void foo() {
        
    }
}

class TestOverrideExecution1Helper extends TestOverrideExecution1 {
    @SCJRestricted(Restrict.ANY_TIME)
    public void foo() {
        
    }
}
