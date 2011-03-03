package scope.scope.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=Scope.IMMORTAL)
@Scope("a")
public abstract class TestBadNewInstanceType extends Mission {
    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends Mission {
        @DefineScope(name="b", parent="a")
        @Scope("a")
        ManagedMemory mem;

        public void foo() {
            try {
                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_TYPE
                mem.newInstance(Y[].class);
                Class<?> c = null;
                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_TYPE
                mem.newInstance(c);
                // TODO: Fix ## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_TYPE
                mem.newInstance(void.class);
                // TODO: Fix ## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_TYPE
                mem.newInstance(int.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Scope("a")
    static class Y { }
}
