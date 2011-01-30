//scope/TestUnannotatedRunnable.java:14: Runnable used with executeInArea() without @RunsIn.
//        a.executeInArea(s);
//                       ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;


@Scope(IMMORTAL)
public class TestUnannotatedRunnable {
    PrivateMemory a = new PrivateMemory(0);

    public void foo() {
        S22 s = new S22();
        a.executeInArea(s);           // ERROR
    }
}

class HelperUnannotatedRunnable {
    void foo() {
        ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new R11());
    }
}

@Scope(IMMORTAL)
@DefineScope(name = "b", parent = IMMORTAL)
class R11 implements Runnable {
    @Override
    @RunsIn("b")
    public void run() {
    }
}

@Scope(IMMORTAL)
@DefineScope(name = "a", parent = IMMORTAL)
class S22 implements Runnable {
    public void run() {                     // error from missing @RunsIn("a")
    }
}