package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.ImmortalMemory;
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
public abstract class TestBadNewInstance extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadNewInstance() {super(null, null);}

    @SCJAllowed(members = true)
    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}


        @DefineScope(name="a", parent=IMMORTAL)
        @Scope(IMMORTAL)
        ManagedMemory mem;

        @DefineScope(name="b", parent="a")
        @Scope("a")
        ManagedMemory mem2;

        public void foo() throws Exception {
            mem.newInstance(Y.class);
            //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE
            mem2.newInstance(Y.class);

            Object o = new Object();
            MemoryArea.getMemoryArea(o).newInstance(Y.class);

            //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE
            ImmortalMemory.instance().newInstance(Y.class);
        }

        @RunsIn("b")
        public void bar() throws Exception {
            ManagedMemory.getCurrentManagedMemory().newInstance(Z.class);
            //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE
            ManagedMemory.getCurrentManagedMemory().newInstance(Y.class);
        }
    }

    @Scope("a")
    static class Y { }

    @Scope("b")
    static class Z { }
}
