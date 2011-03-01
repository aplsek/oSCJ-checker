package scope.scopeRunsIn.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestMemoryAreaNoDefineScope {
    @DefineScope(name="a", parent=Scope.IMMORTAL)
    static abstract class X extends Mission {
        @Scope(Scope.IMMORTAL)
        //## checkers.scope.ScopeRunsInChecker.ERR_MEMORY_AREA_NO_DEFINE_SCOPE
        ManagedMemory mem1;
    }
}
