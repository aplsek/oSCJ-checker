package scope.scope.simple;

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
public abstract class TestMemoryAreaDefineScopeNotConsistent {
    @DefineScope(name="a", parent=IMMORTAL)
    static abstract class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}

    }

    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class Y extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public Y() {super(null, null);}


        public void method() {
            @DefineScope(name="a", parent="b")
            @Scope("a")
            //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT
            ManagedMemory mem;
        }
    }
}
