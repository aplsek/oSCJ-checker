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

@Scope("immortal")
public class TestUnannotatedRunnable {
    @DefineScope(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory(0);

    public void foo() {
        S22 s = new S22();
        a.executeInArea(s);
    }
    
    
}

class HelperUnannotatedRunnable {
    void foo() {
        ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new /*@DefineScope(name="b", parent="immortal")*/ R11());
    }
    
}

@Scope("immortal") @RunsIn("b")
class R11 implements Runnable {
    @Override
    public void run() {
    }
}

@Scope("immortal")
// error from missing @RunsIn("a")
class S22 implements Runnable {
    public void run() {
        
    }
}