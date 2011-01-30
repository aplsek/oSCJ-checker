package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope(IMMORTAL)
@DefineScope(name = "a", parent = IMMORTAL)
public class TestPrivateMemoryAssignment {
    PrivateMemory a = new PrivateMemory(0);
    PrivateMemory b = new PrivateMemory(0);
    
    public void foo() {
        b = a; 
    }
}

@DefineScope(name = "b", parent = IMMORTAL)
class B123 {}