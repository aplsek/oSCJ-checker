//scope/TestPrivateMemoryAssignment.java:16: Cannot assign to a private memory with a different @DefineScope.
//        b = a; // This should fail
//          ^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;

@Scope("immortal")
public class TestPrivateMemoryAssignment {
    @DefineScope(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory(0);
    @DefineScope(name = "b", parent = "immortal")
    PrivateMemory b = new PrivateMemory(0);
    
    public void foo() {
        b = a; // This should fail
    }
}
