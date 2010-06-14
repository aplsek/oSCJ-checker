//scope/TestRunsIn2.java:19: @RunsIn annotations must be a sub-scope of @Scope annotations.
//class RunsInWrong {
//^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("a")
public class TestRunsIn2 {
    @DefineScope(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory(0);
    @DefineScope(name = "b", parent = "immortal")
    PrivateMemory b = new PrivateMemory(0);
}


@Scope("a")
@RunsIn("b")
class RunsInWrong {
    
}