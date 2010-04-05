//scope/TestRunsIn.java:16: Methods must run in the same scope or a child scope of their owning type.
//    public void foo() {
//                ^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("b")
public class TestRunsIn {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
    @ScopeDef(name = "b", parent = "a")
    PrivateMemory b = new PrivateMemory();
    
    @RunsIn("a")
    public void foo() {
        
    }
}
