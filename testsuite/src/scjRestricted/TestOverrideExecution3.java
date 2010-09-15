package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideExecution3 {
    @SCJRestricted(Phase.RUN)
    public void foo() {
        
    }
}

class TestOverrideExecution3Helper extends TestOverrideExecution3 {
    @SCJRestricted(Phase.RUN)
    public void foo() {
        
    }
}
