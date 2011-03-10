package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public abstract class TestMemoryAreaDefineScopeNotConsistentWithScope {
    @DefineScope(name="a", parent=IMMORTAL)
    static abstract class X extends Mission { }

    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class Y extends Mission {
        public void foo() {
            @DefineScope(name="a", parent=IMMORTAL)
            @Scope(IMMORTAL)
            ManagedMemory mem;

            @DefineScope(name="b", parent="a")
            ManagedMemory mem2;

            @DefineScope(name="b", parent="a")
            @Scope(IMMORTAL)
            //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE
            ManagedMemory mem3;

            @DefineScope(name="a", parent=IMMORTAL)
            //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE
            ManagedMemory mem4;

            @DefineScope(name="a", parent=IMMORTAL)
            @Scope("a")
            //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE
            ManagedMemory mem5;
        }
    }
}
