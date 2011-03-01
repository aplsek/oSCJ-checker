package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public abstract class TestMemoryAreaNoDefineScopeOnVar {
    @DefineScope(name="a", parent=IMMORTAL)
    static abstract class X extends Mission { }

    @DefineScope(name="b", parent="a")
    static abstract class Y extends Mission {
        public void method() {
            @Scope(IMMORTAL)
            //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_NO_DEFINE_SCOPE_ON_VAR
            ManagedMemory mem;
        }
    }
}