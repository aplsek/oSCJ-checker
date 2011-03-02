package scope.scope.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=Scope.IMMORTAL)
@Scope("a")
public abstract class TestBadNewInstanceVariable extends Mission {
    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends Mission {
        @DefineScope(name="b", parent="a")
        @Scope("a")
        ManagedMemory mem1 = null;

        public void foo() {
            try {
                @DefineScope(name="b", parent="a")
                @Scope("a")
                ManagedMemory mem = null;

                // ## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE
                mem.newInstance(Y.class);
            } catch (Exception e) { }
        }
    }

    @Scope("a")
    static class Y { }
}