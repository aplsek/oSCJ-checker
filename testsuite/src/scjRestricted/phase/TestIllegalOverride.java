package scjRestricted.phase;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;


public class TestIllegalOverride {
    @SCJRestricted(Phase.INITIALIZATION)
    public void foo() { }

    static class W extends TestIllegalOverride {
        @Override
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_NO_OVERRIDE
        public void foo() { }
    }
}
