package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("a")
@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestBadSCJRunnableBadScope extends Mission {

    @Scope("c")
    @DefineScope(name="c", parent="b")
    static abstract class X extends Mission { }

    @Scope("b")
    @DefineScope(name="b", parent="a")
    static abstract class Y extends Mission {
        @DefineScope(name="a", parent=IMMORTAL)
        @Scope(IMMORTAL)
        ManagedMemory a;

        public void m() {
            Run3 r3 = new Run3();
            //## checkers.scope.ScopeChecker.ERR_SCJ_RUNNABLE_BAD_SCOPE
            a.executeInArea(r3);
        }
    }

    static class Run3 implements SCJRunnable {
        @RunsIn("a")
        public void run() { }
    }
}

