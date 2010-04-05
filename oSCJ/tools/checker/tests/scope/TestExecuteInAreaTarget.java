//scope/TestExecuteInAreaTarget.java:15: executeInArea() must target a parent scope.
//        b.executeInArea(new TestExecuteInAreaTargetRunnable());
//                       ^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("a")
public class TestExecuteInAreaTarget {
    @ScopeDef(name="a", parent="immortal") PrivateMemory a;
    @ScopeDef(name="b", parent="a") PrivateMemory b;

    @RunsIn("a")
    public void foo() {
        b.executeInArea(new TestExecuteInAreaTargetRunnable());
    }
}

@RunsIn("b")
class TestExecuteInAreaTargetRunnable implements Runnable {
    public void run() {
        
    }
}