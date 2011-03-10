package scjRestricted.phase;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestIllegalOverrideAny {
    @SCJRestricted(Phase.ALL)
    public void foo() { }

    static class X extends TestIllegalOverrideAny {
        @Override
        @SCJRestricted(Phase.ALL)
        public void foo() { }
    }

    static class y extends TestIllegalOverrideAny {
        @Override
        @SCJRestricted(Phase.CLEANUP)
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_OVERRIDE
        public void foo() { }
    }

    static class Z extends TestIllegalOverrideAny {
        @Override
        @SCJRestricted(Phase.RUN)
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_OVERRIDE
        public void foo() { }
    }

    static class W extends TestIllegalOverrideAny {
        @Override
        @SCJRestricted(Phase.INITIALIZATION)
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_OVERRIDE
        public void foo() { }
    }
}
