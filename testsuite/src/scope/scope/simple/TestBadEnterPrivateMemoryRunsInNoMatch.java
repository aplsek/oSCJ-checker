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
public abstract class TestBadEnterPrivateMemoryRunsInNoMatch extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadEnterPrivateMemoryRunsInNoMatch() {super(null, null);}

    @RunsIn("a")
    public void bar() {
        Y y = new Y();
        @Scope(IMMORTAL) @DefineScope(name="a", parent=IMMORTAL)
        ManagedMemory mem = null;
        mem.enterPrivateMemory(1000, y);

        mem.enterPrivateMemory(1000, new Y());
    }

    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends MissionSequencer {
        Y y = new Y();

        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}

        @RunsIn("b")
        public void foo() {
            @Scope("a") @DefineScope(name="b", parent="a")
            ManagedMemory mem = null;

            //## checkers.scope.ScopeChecker.ERR_BAD_ENTER_PRIVATE_MEMORY_RUNS_IN_NO_MATCH
            mem.enterPrivateMemory(1000, y);
        }
    }

    @SCJAllowed(members=true)
    @Scope("a")
    @DefineScope(name="c", parent="a")
    static class Y implements Runnable {
        @RunsIn("c")
        public void run() { }
    }
}
