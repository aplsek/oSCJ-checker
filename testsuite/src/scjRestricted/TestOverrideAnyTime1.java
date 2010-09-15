package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestOverrideAnyTime1 {
    @SCJRestricted(Phase.ALL)
    public void foo() {
        
    }
}

class TestOverrideAnyTime1Helper extends TestOverrideAnyTime1 {
    @SCJRestricted(Phase.ALL)
    public void foo() {
        
    }
}