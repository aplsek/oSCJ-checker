package scope.scope.simple;

import javax.realtime.MemoryArea;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=Scope.IMMORTAL)
@Scope("a")
public abstract class TestBadNewInstanceInArea extends Mission {
    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends Mission {

        Y y;

        public void foo() throws InstantiationException, IllegalAccessException {
                MemoryArea.newInstanceInArea(y,Y.class);

                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE
                MemoryArea.newInstanceInArea(y,Z.class);
        }


        @RunsIn("b")
        public void method () throws InstantiationException, IllegalAccessException {
            Z z = new Z();
            //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE
            MemoryArea.newInstanceInArea(z,Y.class);
        }
    }

    @Scope("a")
    static class Y { }

    @Scope("b")
    static class Z { }
}
