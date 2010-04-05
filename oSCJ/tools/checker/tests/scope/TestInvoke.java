//scope/TestInvoke.java:20: Illegal invocation of method of object in scope a while in scope immortal.
//        foo();
//           ^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.AllocFree;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
public class TestInvoke {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
    
    @RunsIn("a")
    public void foo() {
        baz();
    }
    
    public void bar() {
        foo();
    }
    
    @AllocFree
    public void baz() {
        
    }
}
