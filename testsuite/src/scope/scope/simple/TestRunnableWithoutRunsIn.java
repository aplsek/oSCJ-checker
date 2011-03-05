package scope.scope.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;

@Scope("a")
@DefineScope(name="a", parent=Scope.IMMORTAL)
public abstract class TestRunnableWithoutRunsIn extends Mission {

    @Scope("c")
    @DefineScope(name="c", parent="b")
    static abstract class X extends Mission { }

    @Scope("b")
    @DefineScope(name="b", parent="a")
    static abstract class Y extends Mission {
        @DefineScope(name="a", parent=Scope.IMMORTAL)
        @Scope(Scope.IMMORTAL)
        ManagedMemory a;

        @DefineScope(name="c", parent="b")
        @Scope("b")
        ManagedMemory c;

        public void m() {
            Run r = new Run();
            a.executeInArea(r);

            a.executeInArea(new Run());

            Run2 r2 = new Run2();
            //## checkers.scope.ScopeChecker.ERR_RUNNABLE_WITHOUT_RUNS_IN
            a.executeInArea(r2);
        }
    }

    @Scope("b")
    static class Run implements SCJRunnable {
        @RunsIn("a")
        public void run() { }
    }

    @Scope("b")
    static class Run2 implements SCJRunnable {
        public void run() { }
    }
}

