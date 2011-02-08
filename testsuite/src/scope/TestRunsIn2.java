//scope/TestRunsIn2.java:27: Methods must run in the same scope or a child scope of their owning type.
//    public void method() {
//                ^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;


@Scope("a")
@DefineScope(name = "a", parent = IMMORTAL)
public class TestRunsIn2 {
    PrivateMemory a = new PrivateMemory(0);
    PrivateMemory b = new PrivateMemory(0);
}

@Scope("a")
@DefineScope(name = "b", parent = IMMORTAL)
class RunsInWrong {
    @RunsIn("b")
    public void method() {
        
    }
}