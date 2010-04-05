//scope/TestNewInstance.java:21: Cannot allocate objects of type scope.NewInstance inside scope a.
//        a.newInstance(NewInstance.class);
//                     ^
//1 error

package scope;

import javax.realtime.ImmortalMemory;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
class NewInstance {
}

@Scope("a")
public class TestNewInstance {
    @ScopeDef(name="a", parent="immortal")
    public PrivateMemory a;
    
    public void test() {
        ImmortalMemory.instance().newInstance(NewInstance.class);
        a.newInstance(NewInstance.class);
    }
}
