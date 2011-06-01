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

@Scope("D")
@DefineScope(name="D", parent=IMMORTAL)
@SCJAllowed(members = true)
public abstract class TestUpcast2 extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestUpcast2() {
        super(null, null);
    }

    public void bar() {
        Y y = new Y();
        @Scope(IMMORTAL)
        @DefineScope(name = "D", parent = IMMORTAL)
        ManagedMemory mem = null;
        mem.enterPrivateMemory(1000, y);
    }


    @SCJAllowed(members = true)
    @Scope("D")
    @DefineScope(name = "C", parent = "D")
    static class Y implements Runnable {
        @RunsIn("C")
        public void run() {
        }
    }
}