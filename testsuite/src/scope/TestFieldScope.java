//scope/TestFieldScope.java:11: Field must be in the same or parent scope as its owning type.
//    E e = null;
//      ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;

@Scope("immortal")
public class TestFieldScope {
    E e = null;
    
    static class Helper {
        static void foo() {
            ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new /*@DefineScope(name="a", parent="immortal")*/ R12());
        }
    }
}

@Scope("immortal") @RunsIn("a")
class R12 implements Runnable {
    @Override
    public void run() {
    }
}

@Scope("a")
class E {
}
