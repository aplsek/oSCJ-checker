//scope/TestVariableScope.java:12: Variables of type @javax.safetycritical.annotate.Scope("a") D are not allowed in this allocation context (immortal).
//    public void foo(D d) {
//                      ^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
public class TestVariableScope {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
    
    public void foo(D d) {
    }
}

@Scope("a")
class D {
    
}
