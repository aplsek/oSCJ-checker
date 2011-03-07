package scjAllowed;

import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;

import javax.realtime.MemoryArea;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class TestBadInfrastructureCall {
    @SCJAllowed
    public void foo(MemoryArea mem) {
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_INFRASTRUCTURE_CALL
        mem.enter(null);
    }

    @SCJAllowed(INFRASTRUCTURE)
    public void bar(MemoryArea mem) {
        mem.enter(null);
    }
}
