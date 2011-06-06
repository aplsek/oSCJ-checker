package scjAllowed.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.MemoryArea;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed
public class TestBadInfrastructureCall {
    @Scope(IMMORTAL)
    @DefineScope(name="a", parent=IMMORTAL)
    MemoryArea mem;

    @SCJAllowed
    public void foo() {
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_INFRASTRUCTURE_CALL
        mem.enter(null);
    }

    @DefineScope(name="a", parent=IMMORTAL)
    abstract static class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
            // TODO Auto-generated constructor stub
        } }
}
