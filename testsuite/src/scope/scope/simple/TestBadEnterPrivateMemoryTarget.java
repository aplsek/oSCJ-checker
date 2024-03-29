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

@DefineScope(name="a", parent=IMMORTAL)
@Scope(IMMORTAL)
@SCJAllowed(members = true)
public abstract class TestBadEnterPrivateMemoryTarget extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadEnterPrivateMemoryTarget() {super(null, null);}

    @RunsIn("a")
    public void bar() {
        @Scope("a")
        @DefineScope(name="b", parent="a")
        ManagedMemory mem2 = null;
        Z z = new Z();
        //## checkers.scope.ScopeChecker.ERR_BAD_ENTER_PRIVATE_MEMORY_TARGET
        mem2.enterPrivateMemory(1000, z);
    }

    @Scope("a")
    @DefineScope(name="b", parent="a")
    @SCJAllowed(members = true)
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}
    }

    @SCJAllowed(members=true)
    @Scope("a")
    @DefineScope(name="c", parent="a")
    static class Z implements Runnable {
        @RunsIn("c")
        public void run() { }
    }
}
