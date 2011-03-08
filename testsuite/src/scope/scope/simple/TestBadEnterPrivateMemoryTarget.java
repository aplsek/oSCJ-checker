package scope.scope.simple;

import javax.safetycritical.annotate.Scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.RunsIn;

@DefineScope(name = "a", parent = Scope.IMMORTAL)
@Scope("a")
public abstract class TestBadEnterPrivateMemoryTarget extends Mission {
    public void bar() {
        @Scope("a")
        @DefineScope(name = "b", parent = "a")
        ManagedMemory mem2 = null;
        Z z = new Z();
        //## checkers.scope.ScopeChecker.ERR_BAD_ENTER_PRIVATE_MEMORY_TARGET
        mem2.enterPrivateMemory(1000, z);
    }

    @Scope("a")
    @DefineScope(name = "b", parent = "a")
    static abstract class X extends Mission {
    }

    @SCJAllowed(members = true)
    @Scope("a")
    @DefineScope(name = "c", parent = "a")
    static class Z implements SCJRunnable {
        @RunsIn("c")
        public void run() {
        }
    }
}