//scope/TestScopeOnMethod.java:12: @Scope annotations not allowed on methods.
//    public void foo() {
//                ^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

public class TestScopeOnMethod {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
    
    @Scope("a")
    public void foo() {
        
    }
}
