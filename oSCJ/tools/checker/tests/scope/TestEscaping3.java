package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
public class TestEscaping3 {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
    private G g;
    
    @RunsIn("a") public void foo(G g) {
        this.g = g;
        this.g = foo2();
    }
    
    @RunsIn("a")
    public G foo2() {
        return null;
    }
}

@Scope("immortal")
class G {
    
}