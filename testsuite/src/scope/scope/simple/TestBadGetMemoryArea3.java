package scope.scope.simple;

import static checkers.scope.ScopeChecker.ERR_BAD_ALLOCATION_CONTEXT_ASSIGNMENT;
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
@Scope(IMMORTAL)
public abstract class TestBadGetMemoryArea3 extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadGetMemoryArea3() {super(null, null);}

    @SCJAllowed(members = true)
    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class MS extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public MS() {super(null, null);}
    }


    @SCJAllowed(members = true)
    @Scope("a")
    static abstract class X  {

        Y y = new Y();

        Foo foo;

        @RunsIn("b")
        public void m2() throws InstantiationException, IllegalAccessException {
            @Scope(IMMORTAL)
            @DefineScope(name="a", parent=IMMORTAL)
            MemoryArea mem3 = MemoryArea.getMemoryArea(y);

            @Scope(IMMORTAL)
            @DefineScope(name=IMMORTAL, parent=IMMORTAL)
            MemoryArea imm = ImmortalMemory.instance();

            //## checkers.scope.ScopeChecker.ERR_BAD_ALLOCATION_CONTEXT_ASSIGNMENT
            mem3 = imm;

        }

        @RunsIn("b")
        public void method3() {
            @DefineScope(name=IMMORTAL,parent=IMMORTAL)
            @Scope(IMMORTAL)
            MemoryArea m = ManagedMemory.getMemoryArea(foo);

            @DefineScope(name="a",parent=IMMORTAL)
            @Scope(IMMORTAL)
            //## checkers.scope.ScopeChecker.ERR_BAD_ALLOCATION_CONTEXT_ASSIGNMENT
            ImmortalMemory imm = (ImmortalMemory) m;
        }
    }

    @Scope(IMMORTAL)
    static class Foo {}

    @Scope("a")
    static class Y { }

    @Scope("b")
    static class Z { }
}
