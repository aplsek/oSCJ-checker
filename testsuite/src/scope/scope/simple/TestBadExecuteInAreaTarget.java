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
@Scope(IMMORTAL)
@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestBadExecuteInAreaTarget extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadExecuteInAreaTarget() {super(null, null);}

    @Scope("c")
    static abstract class X { }

    @Scope("a")
    @DefineScope(name="b", parent="a")
    @SCJAllowed(members = true)
    static abstract class MS extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public MS() {super(null, null);}
    }

    @Scope("b")
    @DefineScope(name="c", parent="b")
    static abstract class MS2 extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public MS2() {super(null, null);}

    }

    @Scope("b")
    @SCJAllowed(members = true)
    static abstract class Y {

        @DefineScope(name="a", parent=IMMORTAL)
        @Scope(IMMORTAL)
        ManagedMemory a;

        @DefineScope(name="c", parent="b")
        @Scope("b")
        ManagedMemory c;

        @RunsIn("b")
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
    @SCJAllowed(members = true)
    static class Run implements Runnable {
        @RunsIn("a")
        public void run() { }
    }

    @Scope("b")
    @SCJAllowed(members = true)
    static class Run2 implements Runnable {
        @RunsIn("c")
        public void run() { }
    }
}

