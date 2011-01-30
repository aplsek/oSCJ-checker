//scope/TestExecuteInAreaTarget.java:15: executeInArea() must target a parent scope.
//        b.executeInArea(new TestExecuteInAreaTargetRunnable());
//                       ^
//scope/TestExecuteInAreaTarget.java:25: (Class may not have @RunsIn annotation with no @Scope annotation.)
//class TestExecuteInAreaTargetRunnable implements Runnable {
//^
//2 errors

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope("a")
@DefineScope(name="a", parent=IMMORTAL)
public class TestExecuteInAreaTarget {
    PrivateMemory a;
    PrivateMemory b;

    @RunsIn("a")
    public void foo() {
        b.executeInArea(new TestExecuteInAreaTargetRunnable());
    }
}


@DefineScope(name="b", parent="a")
class TestExecuteInAreaTargetRunnable implements Runnable {
    @RunsIn("b")
    public void run() {
    }
}