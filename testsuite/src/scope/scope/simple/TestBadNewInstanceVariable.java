package scope.scope.simple;

import javax.safetycritical.annotate.Scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;

@DefineScope(name="a",parent=Scope.IMMORTAL)
@Scope("a")
public abstract class TestBadNewInstanceVariable extends Mission  {
    @Scope("a")
    @DefineScope(name="b",parent="a")
    abstract class X extends Mission {
        public void method() {
            try {
                @DefineScope(name="b",parent="a")
                @Scope("a")
                ManagedMemory mem = null;
                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE
                mem.newInstance(MyFoo2.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Scope("a")
    class MyFoo2 {}
}

