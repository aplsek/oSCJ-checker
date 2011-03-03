package scope.scope.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=Scope.IMMORTAL)
@Scope("a")
public abstract class TestMemoryAreaDefineScopeNotConsistentVariable extends
        Mission {

    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends Mission {
        public void foo() {
            @DefineScope(name = "a", parent = "b")
            @Scope("a")
            //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT
            ManagedMemory mem;
        }
    }
}
