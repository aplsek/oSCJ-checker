package scope.scopeRunsIn.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestMemoryAreaDefineScopeNotConsistentWithScope {
    @DefineScope(name="a", parent=Scope.IMMORTAL)
    static abstract class X extends Mission {
        @Scope(Scope.IMMORTAL)
        @DefineScope(name="a", parent=Scope.IMMORTAL)
        ManagedMemory mem1;
    }
    @DefineScope(name="b", parent="a")
    @Scope(Scope.IMMORTAL)
    static abstract class Y extends Mission {
        @DefineScope(name="b", parent="a")
        //## checkers.scope.ScopeRunsInChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE
        ManagedMemory mem1;
    }
}
