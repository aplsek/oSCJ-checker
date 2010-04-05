package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
public class TestEscaping {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
    private M m;
    
    @RunsIn("a") public void foo(M m) {
        this.m = m; // fails because the parameter m may not reside in immortal
    }
}

class M {
    
}