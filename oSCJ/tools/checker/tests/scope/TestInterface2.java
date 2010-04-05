//scope/TestInterface2.java:20: Variables of type @javax.safetycritical.annotate.Scope("a") A are not allowed in this allocation context (immortal).
//        A a = new A();
//          ^
//1 error

package scope;

import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
interface Test2 {
    void run();
}

@Scope("immortal")
public class TestInterface2 implements Test2 {
    @Scope("a")
    class A {
        
    }
    @ScopeDef(name="a", parent="immortal")
    int a;
    public void run() {
        A a = new A();
    }
}
