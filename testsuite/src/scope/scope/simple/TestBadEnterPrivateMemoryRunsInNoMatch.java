package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
@Scope("a")
public abstract class TestBadEnterPrivateMemoryRunsInNoMatch extends Mission {
    public void bar() {
        Y y = new Y();
        @Scope(IMMORTAL) @DefineScope(name="a", parent=IMMORTAL)
        ManagedMemory mem = null;
        mem.enterPrivateMemory(1000, y);

        mem.enterPrivateMemory(1000, new Y());
    }

    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends Mission {
        Y y = new Y();

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
    static class Y implements SCJRunnable {
        @RunsIn("c")
        public void run() { }
    }
}
