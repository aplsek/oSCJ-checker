package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("immortal")
public class TestSimple {
    @RunsIn("a")
    public void a() {
        B b = new B();
        b.foo();
    }
    
    static class Helper {
        static void foo() {
            ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new /*@DefineScope(name="a", parent="immortal")*/ R1());
        }
        @Scope("immortal") @RunsIn("a")
        static class R1 implements Runnable {
            @Override
            public void run() {
                ManagedMemory.
                getCurrentManagedMemory().
                    enterPrivateMemory(0, new /*@DefineScope(name="b", parent="a")*/ R2());
            }
        }
        @Scope("a") @RunsIn("b")
        static class R2 implements Runnable {
            @Override
            public void run() {
            }
        }
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