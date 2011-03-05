package scope.scope.simple;

import javax.safetycritical.annotate.Scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.RunsIn;

@DefineScope(name="a", parent=Scope.IMMORTAL)
@Scope("a")
public abstract class TestBadEnterPrivateMemoryRunsInNoMatch extends Mission {
    public void bar() {
        Y y = new Y();
        @Scope(Scope.IMMORTAL) @DefineScope(name="a", parent=Scope.IMMORTAL)
        ManagedMemory mem = null;
        mem.enterPrivateMemory(1000, y);
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
