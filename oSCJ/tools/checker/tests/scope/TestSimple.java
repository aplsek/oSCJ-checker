package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
public class TestSimple {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
    @ScopeDef(name = "b", parent = "a")
    PrivateMemory b = new PrivateMemory();
    
    @RunsIn("a")
    public void a() {
        B b = new B();
        b.foo();
    }
}

@Scope("a")
class A {
    public void foo() {

    }
}

class B extends A {
    @Override
    public void foo() {
        super.foo();
    }
}
