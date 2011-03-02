package scope.scopeVisitor.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.SCJRestricted;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

@DefineScope(name="a", parent=IMMORTAL)
@Scope("a")
public abstract class TestMemoryAreaNoDefineScopeOnVar extends Mission {
    @Scope("a")
    @DefineScope(name="b", parent="a")
    abstract class X extends Mission {
        public void foo() {
            @DefineScope(name="a", parent=IMMORTAL)
            @Scope(IMMORTAL)
            ManagedMemory mem;

            @DefineScope(name="b", parent="a")
            @Scope("a")
            ManagedMemory mem2;

            @DefineScope(name="b", parent="a")
            ManagedMemory mem3;

            @Scope(IMMORTAL)
            //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_NO_DEFINE_SCOPE_ON_VAR
            ManagedMemory mem4;
        }
    }
}