package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

public class TestUpcast {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
}

@Scope("a")
class L extends TestUpcast {
    public void foo() {
        TestUpcast f = (TestUpcast) new L(); // should fail
        TestUpcast g = new L(); // should fail
    }
}
