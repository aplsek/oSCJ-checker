//scope/TestRunsIn2.java:19: @RunsIn annotations must be a sub-scope of @Scope annotations.
//class RunsInWrong {
//^
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
@RunsIn("b")
@DefineScope(name = "b", parent = IMMORTAL)
class RunsInWrong {
}