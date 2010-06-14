package scjRestricted;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideExecution3 {
    @SCJRestricted(Restrict.EXECUTION)
    public void foo() {
        
    }
}

class TestOverrideExecution3Helper extends TestOverrideExecution3 {
    @SCJRestricted(Restrict.EXECUTION)
    public void foo() {
        
    }
}
