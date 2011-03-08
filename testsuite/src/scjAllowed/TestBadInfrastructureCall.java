package scjAllowed;

import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;

import javax.realtime.MemoryArea;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@SCJAllowed
public class TestBadInfrastructureCall {
    @Scope(Scope.IMMORTAL)
    @DefineScope(name = "a", parent = Scope.IMMORTAL)
    MemoryArea mem;

    @SCJAllowed
    public void foo() {
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_INFRASTRUCTURE_CALL
        mem.enter(null);
    }

    @SCJAllowed(INFRASTRUCTURE)
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_USER_LEVEL
    public void bar() {
        mem.enter(null);
    }

    @DefineScope(name = "a", parent = Scope.IMMORTAL)
    abstract static class X extends Mission {
    }
}
