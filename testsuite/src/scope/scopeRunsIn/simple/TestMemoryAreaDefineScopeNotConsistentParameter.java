package scope.scopeRunsIn.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestMemoryAreaDefineScopeNotConsistentParameter {
    @DefineScope(name="a", parent=Scope.IMMORTAL)
    static abstract class X extends Mission {
        //## checkers.scope.ScopeRunsInChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT
        void foo(@Scope(Scope.IMMORTAL) @DefineScope(name="a", parent="b") ManagedMemory mem1) { }
    }
    @DefineScope(name="b", parent="a")
    static abstract class Y extends Mission { }
}
