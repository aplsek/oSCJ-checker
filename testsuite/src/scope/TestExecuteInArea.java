package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;

import static checkers.scope.ScopeChecker.ERR_BAD_EXECUTE_IN_AREA_OR_ENTER;
import static checkers.scope.ScopeChecker.ERR_BAD_EXECUTE_IN_AREA_TARGET;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope("a")
@DefineScope(name = "a", parent = IMMORTAL)
public abstract class TestExecuteInArea extends Mission {

    @Scope("c")
    @DefineScope(name = "c", parent = "b")
    static abstract class X extends Mission {}

    @Scope("b")
    @DefineScope(name = "b", parent = "a")
    abstract static class x extends Mission {

        @DefineScope(name = "a", parent = IMMORTAL)
        @Scope(IMMORTAL)
        ManagedMemory a;

        @DefineScope(name = "c", parent = "b")
        @Scope("b")
        ManagedMemory c;

        public void m() {
            Run r = new Run();
            a.executeInArea(r);

            Run2 r2 = new Run2();
            a.executeInArea(r2);

            Run3 r3 = new Run3();
            a.executeInArea(r3);

            Run4 r4 = new Run4();
            c.executeInArea(r4);
        }
    }

    @Scope("b")
    static class Run implements SCJRunnable {
        @RunsIn("a")
        public void run() {
        }
    }

    @Scope("b")
    static class Run2 implements SCJRunnable {
        //## checkers.scope.ScopeChecker.ERR_BAD_EXECUTE_IN_AREA_OR_ENTER
        @RunsIn("b")
        public void run() {
        }
    }

    @Scope("b")
    static class Run3 implements SCJRunnable {
        //## checkers.scope.ScopeChecker.ERR_RUNNABLE_WITHOUT_RUNS_IN
        public void run() {
        }
    }

    @Scope("b")
    static class Run4 implements SCJRunnable {
        //## checkers.scope.ScopeChecker.ERR_BAD_EXECUTE_IN_AREA_TARGET
        @RunsIn("c")
        public void run() {
        }
    }
}

