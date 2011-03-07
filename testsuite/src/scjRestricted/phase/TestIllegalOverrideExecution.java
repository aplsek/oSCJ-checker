package scjRestricted.phase;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestIllegalOverrideExecution {
    @SCJRestricted(Phase.RUN)
    public void foo() { }

    static class X extends TestIllegalOverrideExecution {
        @Override
        @SCJRestricted(Phase.ALL)
        public void foo() { }
    }

    static class Y extends TestIllegalOverrideExecution {
        @Override
        @SCJRestricted(Phase.CLEANUP)
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_OVERRIDE
        public void foo() { }
    }

    static class Z extends TestIllegalOverrideExecution {
        @Override
        @SCJRestricted(Phase.RUN)
        public void foo() { }
    }

    static class W extends TestIllegalOverrideExecution {
        @Override
        @SCJRestricted(Phase.INITIALIZATION)
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_OVERRIDE
        public void foo() { }
    }
}
