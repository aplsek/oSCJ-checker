package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
@Scope("A")
@DefineScope(name="A", parent=IMMORTAL)
public abstract class TestBadExecuteInAreaRunsIn extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadExecuteInAreaRunsIn() {super(null, null);}

    @Scope("C")
    @DefineScope(name="C", parent="B")
    static abstract class X extends Mission { }

    @SCJAllowed(members = true)
    @Scope("B")
    @DefineScope(name="B", parent="A")
    static abstract class Y extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public Y() {super(null, null);}

        @DefineScope(name="A", parent=IMMORTAL)
        @Scope(IMMORTAL)
        ManagedMemory a;

        @DefineScope(name="C", parent="B")
        @Scope("B")
        ManagedMemory c;

        public void m() {
            Run r = new Run();
            a.executeInArea(r);
            a.executeInArea(new Run());

            Run2 r2 = new Run2();
            //## checkers.scope.ScopeChecker.ERR_BAD_EXECUTE_IN_AREA_RUNS_IN
            a.executeInArea(r2);
        }
    }

    @Scope("B")
    static class Run implements Runnable {
        @RunsIn("A")
        public void run() { }
    }
    static class Run2 implements Runnable {
        @RunsIn("B")
        public void run() { }
    }
}

