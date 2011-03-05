package scope.scope.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=Scope.IMMORTAL)
@Scope("a")
public abstract class TestGetCurrentManagedMemory extends Mission {

    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends Mission {
        Y y = new Y();

        public void foo() throws InstantiationException, IllegalAccessException {
            ManagedMemory.getCurrentManagedMemory();

            @Scope(Scope.IMMORTAL)
            @DefineScope(name="a", parent=Scope.IMMORTAL)
            ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
        }

        @RunsIn("b")
        public void bar() throws InstantiationException, IllegalAccessException {
            ManagedMemory.getCurrentManagedMemory();

            @Scope("a")
            @DefineScope(name="b", parent="a")
            ManagedMemory mem2 = ManagedMemory.getCurrentManagedMemory();

            @Scope("b")
            @DefineScope(name="b", parent="a")
            //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE
            ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();

        }
    }

    @Scope("a")
    static class Y { }

    @Scope("b")
    static class Z { }
}