//scope/TestConstructor.java:14: @RunsIn annotations not allowed on constructors.
//    public TestConstructor() {
//           ^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
public class TestConstructor {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
    
    @RunsIn("a")
    public TestConstructor() {
        
    }
}
