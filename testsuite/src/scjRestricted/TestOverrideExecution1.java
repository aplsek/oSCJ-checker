package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideExecution1 {
    @SCJRestricted(Phase.RUN)
    public void foo() {
        
    }
}

class TestOverrideExecution1Helper extends TestOverrideExecution1 {
    @SCJRestricted(Phase.ALL)
    public void foo() {
        
    }
}
