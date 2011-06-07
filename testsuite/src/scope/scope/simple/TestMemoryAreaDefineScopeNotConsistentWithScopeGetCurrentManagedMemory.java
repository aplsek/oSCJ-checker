package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
@DefineScope(name="a", parent=IMMORTAL)
@Scope(IMMORTAL)
public abstract class TestMemoryAreaDefineScopeNotConsistentWithScopeGetCurrentManagedMemory
        extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestMemoryAreaDefineScopeNotConsistentWithScopeGetCurrentManagedMemory() {super(null, null);}

    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}

        public void foo() throws InstantiationException, IllegalAccessException {
            @Scope(IMMORTAL)
            @DefineScope(name="a", parent=IMMORTAL)
            ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
        }

        @RunsIn("b")
        public void bar() throws InstantiationException, IllegalAccessException {
            @Scope("a")
            @DefineScope(name="b", parent="a")
            ManagedMemory mem2 = ManagedMemory.getCurrentManagedMemory();

            @Scope("b")
            @DefineScope(name="b", parent="a")
            //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE
            ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();

        }
    }
}
