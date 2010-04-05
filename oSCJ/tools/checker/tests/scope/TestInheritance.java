//scope/TestInheritance.java:23: @Scope annotations must agree with their overridden annotations.
//class H extends TestInheritance {
//^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
@RunsIn("a")
public class TestInheritance {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
    @ScopeDef(name = "b", parent = "a")
    PrivateMemory b = new PrivateMemory();
    
    public void foo() {
        
    }
}

@Scope("a")
@RunsIn("b")
class H extends TestInheritance {
    
}

@Scope("immortal")
@RunsIn("a")
class I extends TestInheritance {
    @Override
    @RunsIn("b")
    public void foo() {
        
    }
}