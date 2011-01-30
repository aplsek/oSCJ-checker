package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope(IMMORTAL)
public class TestEscaping3 {
    private G g;
    
    @RunsIn("a") public void foo(G g) {
        this.g = g;
        this.g = foo2();
    }
    
    @RunsIn("a")
    public G foo2() {
        return null;
    }
    
    static class Helper {
        static void foo() {
            ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new R1());
        }
        @Scope(IMMORTAL) 
        @DefineScope(name="a", parent=IMMORTAL)
        static class R1 implements Runnable {
            @Override
            @RunsIn("a")
            public void run() {
            }
        }
    }
}

@Scope(IMMORTAL)
class G {
}