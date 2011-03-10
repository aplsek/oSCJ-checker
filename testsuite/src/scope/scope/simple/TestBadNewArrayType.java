package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
@Scope("a")
public abstract class TestBadNewArrayType extends Mission {
    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends Mission {
        @DefineScope(name="b", parent="a")
        @Scope("a")
        ManagedMemory mem;

        public void foo() {
            try {
                mem.newArray(Y[].class, 1);
                Class<?> c = null;
                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_ARRAY_TYPE
                mem.newArray(c, 1);
                // TODO: FIX ## checkers.scope.ScopeChecker.ERR_BAD_NEW_ARRAY_TYPE
                //mem.newArray(void.class, 1);
                mem.newArray(int.class, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Scope("b")
    static class Y { }
}
