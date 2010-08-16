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

@Scope("a")
public class TestExecuteInAreaTarget {
    @DefineScope(name="a", parent="immortal") PrivateMemory a;
    @DefineScope(name="b", parent="a") PrivateMemory b;

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