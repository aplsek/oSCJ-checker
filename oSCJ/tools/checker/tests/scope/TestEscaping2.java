package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
public class TestEscaping2 {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
    J j;
}

class J {
    
}

@Scope("immortal")
class K {
    @RunsIn("a") public void foo(TestEscaping2 t) {
        J j = t.j; // disallowed because foo does not run in immortal
    }

    public void bar(TestEscaping2 t) {
        J j2 = t.j; // allowed because bar runs in immortal
    }
}