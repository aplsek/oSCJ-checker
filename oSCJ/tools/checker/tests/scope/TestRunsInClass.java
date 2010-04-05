//scope/TestRunsInClass.java:9: @RunsIn annotations must be a sub-scope of @Scope annotations.
//public class TestRunsInClass {
//       ^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("b") @RunsIn("a")
public class TestRunsInClass {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
    @ScopeDef(name = "b", parent = "a")
    PrivateMemory b = new PrivateMemory();
    
    public void foo() {
        
    }
}
