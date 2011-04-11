package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestBadAllocationContextAssignment extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadAllocationContextAssignment() {super(null, null);}

    @Scope("a")
    @DefineScope(name="b", parent=IMMORTAL)
    static abstract class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}

        @Scope(IMMORTAL)
        @DefineScope(name="a", parent=IMMORTAL)
        ManagedMemory a;

        @Scope(IMMORTAL)
        @DefineScope(name="a", parent=IMMORTAL)
        ManagedMemory a2 = a;

        @Scope(IMMORTAL)
        @DefineScope(name="b", parent=IMMORTAL)
        //## checkers.scope.ScopeChecker.ERR_BAD_ALLOCATION_CONTEXT_ASSIGNMENT
        ManagedMemory b = a;
    }
}
