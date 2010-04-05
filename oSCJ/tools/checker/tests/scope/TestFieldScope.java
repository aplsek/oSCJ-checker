//scope/TestFieldScope.java:11: Field must be in the same or parent scope as its owning type.
//    E e = null;
//      ^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
public class TestFieldScope {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
    E e = null;
}

@Scope("a")
class E {
    
}
