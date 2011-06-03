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
@Scope("a")
@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestBadRunnableBadScope extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadRunnableBadScope() {super(null, null);}

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

        public void m() {
            Run3 r3 = new Run3();
            a.executeInArea(r3);
        }
    }

    static class Run3 implements Runnable {
        @RunsIn("a")
        public void run() { }
    }
}

