//scope/TestAllocation.java:13: Object allocation in a context (immortal) other than its designated scope (a).
//        new C();
//        ^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
public class TestAllocation {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
    
    public void foo() {
        new C();
    }
}

@Scope("a")
class C {
    
}
