package scjRestricted.phase;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestIllegalOverrideInitialize {
    @SCJRestricted(Phase.INITIALIZATION)
    public void foo() { }

    static class X extends TestIllegalOverrideInitialize {
        @Override
        @SCJRestricted(Phase.ALL)
        public void foo() { }
    }

    static class Y extends TestIllegalOverrideInitialize {
        @Override
        @SCJRestricted(Phase.CLEANUP)
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_OVERRIDE
        public void foo() { }
    }

    static class Z extends TestIllegalOverrideInitialize {
        @Override
        @SCJRestricted(Phase.RUN)
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_OVERRIDE
        public void foo() { }
    }

    static class W extends TestIllegalOverrideInitialize {
        @Override
        @SCJRestricted(Phase.INITIALIZATION)
        public void foo() { }
    }
}
