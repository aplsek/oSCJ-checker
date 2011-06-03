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
@DefineScope(name="A", parent=IMMORTAL)
@Scope("A")
public abstract class TestEnterPrivateMemory extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestEnterPrivateMemory() {super(null, null);}

    public void bar() {
        Y y = new Y();
        @Scope(IMMORTAL) @DefineScope(name="A", parent=IMMORTAL)
        ManagedMemory mem = null;
        mem.enterPrivateMemory(1000, y);
    }

    @SCJAllowed(members=true)
    @Scope("A")
    @DefineScope(name="B", parent="A")
    static class Y implements Runnable {
        @RunsIn("B")
        public void run() { }
    }

}
