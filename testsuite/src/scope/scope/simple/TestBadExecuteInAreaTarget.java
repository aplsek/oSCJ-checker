package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope("a")
@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestBadExecuteInAreaTarget extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadExecuteInAreaTarget() {super(null, null);}

    @Scope("c")
    @DefineScope(name="c", parent="b")
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}

    }

    @Scope("b")
    @DefineScope(name="b", parent="a")
    static abstract class Y extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public Y() {super(null, null);}

        @DefineScope(name="a", parent=IMMORTAL)
        @Scope(IMMORTAL)
        ManagedMemory a;

        @DefineScope(name="c", parent="b")
        @Scope("b")
        ManagedMemory c;

        public void m() {
            Run r = new Run();
            a.executeInArea(r);
            a.executeInArea(new Run());

            Run2 r2 = new Run2();
            //## checkers.scope.ScopeChecker.ERR_BAD_EXECUTE_IN_AREA_TARGET
            c.executeInArea(r2);
        }
    }

    @Scope("b")
    static class Run implements SCJRunnable {
        @RunsIn("a")
        public void run() { }
    }

    @Scope("b")
    static class Run2 implements SCJRunnable {
        @RunsIn("c")
        public void run() { }
    }
}

