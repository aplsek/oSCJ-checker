package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
public class TestMemoryAreaDefineScopeNotConsistentWithScopeParameter {
    @DefineScope(name="a", parent=IMMORTAL)
    static abstract class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}

        void foo(@Scope(IMMORTAL) @DefineScope(name="a", parent=IMMORTAL) ManagedMemory mem1) { }
    }
    @DefineScope(name="b", parent="a")
    @Scope(IMMORTAL)
    static abstract class Y extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public Y() {super(null, null);}

        //## checkers.scope.ScopeRunsInChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE
        void foo(@DefineScope(name="b", parent="a") ManagedMemory mem1) { }
    }
}
