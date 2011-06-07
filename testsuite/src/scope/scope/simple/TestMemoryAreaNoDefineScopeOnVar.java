package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
@DefineScope(name="a", parent=IMMORTAL)
@Scope(IMMORTAL)
public abstract class TestMemoryAreaNoDefineScopeOnVar extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestMemoryAreaNoDefineScopeOnVar() {super(null, null);}

    @Scope("a")
    @DefineScope(name="b", parent="a")
    abstract class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}

        public void foo() {
            @DefineScope(name="a", parent=IMMORTAL)
            @Scope(IMMORTAL)
            ManagedMemory mem;

            @DefineScope(name="b", parent="a")
            @Scope("a")
            ManagedMemory mem2;

            @DefineScope(name="b", parent="a")
            ManagedMemory mem3;
        }

        @RunsIn("b")
        void bar() {
            @Scope(IMMORTAL)
            //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_NO_DEFINE_SCOPE_ON_VAR
            ManagedMemory mem = null;
        }
    }

    @SCJAllowed(members=true)
    @Scope("b")
    @DefineScope(name="c", parent="b")
    static class Y implements Runnable {
        @RunsIn("c")
        public void run() { }
    }
}
