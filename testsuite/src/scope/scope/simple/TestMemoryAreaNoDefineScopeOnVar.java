package scope.scope.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=Scope.IMMORTAL)
@Scope("a")
public abstract class TestMemoryAreaNoDefineScopeOnVar extends Mission {
    @Scope("a")
    @DefineScope(name="b", parent="a")
    abstract class X extends Mission {
        public void foo() {
            @DefineScope(name="a", parent=Scope.IMMORTAL)
            @Scope(Scope.IMMORTAL)
            ManagedMemory mem;

            @DefineScope(name="b", parent="a")
            @Scope("a")
            ManagedMemory mem2;

            @DefineScope(name="b", parent="a")
            ManagedMemory mem3;
        }

        @RunsIn("b")
        void bar() {
            @Scope(Scope.IMMORTAL)
            //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_NO_DEFINE_SCOPE_ON_VAR
            ManagedMemory mem = null;
        }
    }



    @SCJAllowed(members=true)
    @Scope("b")
    @DefineScope(name="c", parent="b")
    static class Y implements SCJRunnable {
        @RunsIn("c")
        public void run() { }
    }
}