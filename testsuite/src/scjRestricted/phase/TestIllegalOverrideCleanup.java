package scjRestricted.phase;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestIllegalOverrideCleanup {
    @SCJRestricted(Phase.CLEANUP)
    public void foo() { }

    static class X extends TestIllegalOverrideCleanup {
        @Override
        @SCJRestricted(Phase.ALL)
        public void foo() { }
    }

    static class Y extends TestIllegalOverrideCleanup {
        @Override
        @SCJRestricted(Phase.CLEANUP)
        public void foo() { }
    }

    static class Z extends TestIllegalOverrideCleanup {
        @Override
        @SCJRestricted(Phase.RUN)
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_OVERRIDE
        public void foo() { }
    }

    static class W extends TestIllegalOverrideCleanup {
        @Override
        @SCJRestricted(Phase.INITIALIZATION)
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_OVERRIDE
        public void foo() { }
    }
}