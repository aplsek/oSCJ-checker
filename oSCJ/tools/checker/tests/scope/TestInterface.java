package scope;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
interface Test {
    @RunsIn("a")
    void run();
}

@Scope("immortal")
public class TestInterface implements Test {
    @Scope("a")
    class A {
        
    }
    @ScopeDef(name="a", parent="immortal")
    int a;
    public void run() {
        A a = new A();
    }
}
