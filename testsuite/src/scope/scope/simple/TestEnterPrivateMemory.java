package scope.scope.simple;

import javax.safetycritical.annotate.Scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.RunsIn;

@DefineScope(name="a",parent=Scope.IMMORTAL)
@Scope("a")
public abstract class TestEnterPrivateMemory extends Mission {

    public void bar1() {
        RunY runY = new RunY();
        @Scope(Scope.IMMORTAL) @DefineScope(name="a",parent=Scope.IMMORTAL)
        ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
        mem.enterPrivateMemory(1000, runY);
    }

    @Scope("a")
    @DefineScope(name="b",parent="a")
    static abstract class PEH extends Mission {
        @RunsIn("b")
        public void handleAsyncEvent() {
            RunX runX = new RunX();
            @Scope("a") @DefineScope(name="b",parent="a")
            ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
            mem.enterPrivateMemory(1000, runX);

            mem.enterPrivateMemory(1000, new RunX());
            method(new RunX());

            ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, runX);

            RunY runY = new RunY();
            //## checkers.scope.ScopeChecker.ERR_BAD_ENTER_PRIVATE_MEMORY_RUNS_IN_NO_MATCH
            mem.enterPrivateMemory(1000, runY);

            RunZ runZ = new RunZ();
            //## checkers.scope.ScopeChecker.ERR_BAD_ENTER_PRIVATE_MEMORY_RUNS_IN_NO_MATCH
            mem.enterPrivateMemory(1000, runZ);
        }

        @RunsIn("b")
        public void method(RunX runX) {
            ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
            mem.enterPrivateMemory(1000, runX);
        }
    }

    @SCJAllowed(members=true)
    @Scope("b")
    @DefineScope(name="runX",parent="b")
    static class RunX implements SCJRunnable {
        @RunsIn("runX")
        public void run() {
        }
    }

    @SCJAllowed(members=true)
    @Scope("a")
    @DefineScope(name="runY",parent="a")
    static class RunY implements SCJRunnable {
        @RunsIn("runY")
        public void run() {
        }
    }

    @SCJAllowed(members=true)
    @Scope("a")
    @DefineScope(name="runZ",parent="a")
    static  class RunZ implements SCJRunnable {
        public void run() {
        }
    }
}
