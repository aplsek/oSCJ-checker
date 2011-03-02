package scope.scopeVisitor.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=Scope.IMMORTAL)
@Scope("a")
public abstract class TestMemoryAreaDefineScopeNotConsistentWithScopeVariable extends
        Mission {
    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends Mission {
        public void foo() {
            @DefineScope(name="b", parent="a")
            @Scope(Scope.IMMORTAL)
            //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE
            ManagedMemory mem;

            @DefineScope(name="a", parent=Scope.IMMORTAL)
            //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE
            ManagedMemory mem2;

            @DefineScope(name="a", parent=Scope.IMMORTAL)
            @Scope("a")
            //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE
            ManagedMemory mem3;
        }
    }
}