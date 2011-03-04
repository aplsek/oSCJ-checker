package scope.scope.simple;

import javax.safetycritical.annotate.Scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.RunsIn;

@DefineScope(name="a",parent=Scope.IMMORTAL)
@Scope("a")
public abstract class TestEnterPrivateMemory2 extends Mission {

    @Scope("a")
    @DefineScope(name="b",parent="a")
    static abstract class PEH extends Mission {
        @RunsIn("b")
        public void handleAsyncEvent() {
            RunX runX = new RunX();

            ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, runX);
        }
    }

    @SCJAllowed(members=true)
    @Scope("b")
    @DefineScope(name="runX",parent="b")
    static class RunX implements SCJRunnable {
        @RunsIn("runX")
        public void run() {
        }
    }
}
