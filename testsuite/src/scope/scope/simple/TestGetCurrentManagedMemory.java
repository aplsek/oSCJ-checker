package scope.scope.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=Scope.IMMORTAL)
@Scope("a")
public class TestGetCurrentManagedMemory {

    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends Mission {

        @RunsIn("b")
        public void bar() throws InstantiationException, IllegalAccessException {
            ManagedMemory.getCurrentManagedMemory();

            @Scope("b")
            @DefineScope(name="b", parent="a")
            ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();

        }
    }

    @Scope("a")
    static class Y { }

    @Scope("b")
    static class Z { }
}