//scope/TestEnterTarget.java:15: enter() must target a child scope.
//        a.enter(new TestEnterTargetRunnable());
//               ^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("a")
public class TestEnterTarget {
    @ScopeDef(name="a", parent="immortal") PrivateMemory a;
    @ScopeDef(name="b", parent="a") PrivateMemory b;

    @RunsIn("b")
    public void foo() {
        a.enter(new TestEnterTargetRunnable());
    }
}

@RunsIn("a")
class TestEnterTargetRunnable implements Runnable {
    public void run() {
        
    }
}