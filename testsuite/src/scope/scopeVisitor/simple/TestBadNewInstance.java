package scope.scopeVisitor.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=Scope.IMMORTAL)
@Scope("a")
public abstract class TestBadNewInstance extends Mission {
    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends Mission {
        @DefineScope(name="b", parent="a")
        @Scope("a")
        ManagedMemory mem;

        @DefineScope(name="a", parent=Scope.IMMORTAL)
        @Scope(Scope.IMMORTAL)
        ManagedMemory mem2;

        public void foo() {
            try {
                mem2.newInstance(Y.class);
                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE
                mem.newInstance(Y.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Scope("a")
    static class Y { }
}
