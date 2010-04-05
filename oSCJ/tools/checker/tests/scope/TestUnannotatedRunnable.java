//scope/TestUnannotatedRunnable.java:14: Runnable used with executeInArea() without @RunsIn.
//        a.executeInArea(s);
//                       ^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
public class TestUnannotatedRunnable {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();

    public void foo() {
        S s = new S();
        a.executeInArea(s);
    }
}

@Scope("immortal")
// error from missing @RunsIn("a")
class S implements Runnable {
    public void run() {
        
    }
}