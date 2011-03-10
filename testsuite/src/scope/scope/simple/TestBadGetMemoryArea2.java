package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.MemoryArea;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
@Scope("a")
public abstract class TestBadGetMemoryArea2 extends Mission {
    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends Mission {

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
