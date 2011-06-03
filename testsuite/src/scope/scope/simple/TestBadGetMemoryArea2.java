package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.MemoryArea;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
@DefineScope(name="a", parent=IMMORTAL)
@Scope("a")
public abstract class TestBadGetMemoryArea2 extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadGetMemoryArea2() {super(null, null);}

    @SCJAllowed(members = true)
    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}

        Y y = new Y();

        @RunsIn("b")
        public void m2() throws InstantiationException, IllegalAccessException {
            @Scope("a")
            @DefineScope(name="b", parent="a")
            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            ManagedMemory mem3 = (ManagedMemory) MemoryArea.getMemoryArea(y);
        }
    }

    @Scope("a")
    static class Y { }

    @Scope("b")
    static class Z { }
}
