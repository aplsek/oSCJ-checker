package scope.scopeRunsIn.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestMemoryAreaDefineScopeNotConsistentWithScopeParameter {
    @DefineScope(name="a", parent=Scope.IMMORTAL)
    static abstract class X extends Mission {
        void foo(@Scope(Scope.IMMORTAL) @DefineScope(name="a", parent=Scope.IMMORTAL) ManagedMemory mem1) { }
    }
    @DefineScope(name="b", parent="a")
    @Scope(Scope.IMMORTAL)
    static abstract class Y extends Mission {
        //## checkers.scope.ScopeRunsInChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE
        void foo(@DefineScope(name="b", parent="a") ManagedMemory mem1) { }
    }
}
