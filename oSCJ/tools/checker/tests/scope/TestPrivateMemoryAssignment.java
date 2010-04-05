//scope/TestPrivateMemoryAssignment.java:16: Cannot assign to a private memory with a different @ScopeDef.
//        b = a; // This should fail
//          ^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
public class TestPrivateMemoryAssignment {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
    @ScopeDef(name = "b", parent = "immortal")
    PrivateMemory b = new PrivateMemory();
    
    public void foo() {
        b = a; // This should fail
    }
}
