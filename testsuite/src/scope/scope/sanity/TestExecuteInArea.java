package scope.scope.sanity;

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
@DefineScope(name="A", parent=IMMORTAL)
public abstract class TestExecuteInArea extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestExecuteInArea() {super(null, null);}

    @SCJAllowed(members = true)
    @Scope("A")
    @DefineScope(name="B", parent="A")
    static abstract class Y extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public Y() {super(null, null);}

        @DefineScope(name="A", parent=IMMORTAL)
        @Scope(IMMORTAL)
        ManagedMemory a;

        @RunsIn("B")
        public void m() {
            Run r = new Run();
            a.executeInArea(r);
        }
    }

    @Scope("B")
    static class Run implements Runnable {
        @RunsIn("A")
        public void run() { }
    }
}

