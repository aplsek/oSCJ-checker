//scope/TestEscaping4.java:17: Variables of type Bar are not allowed in this allocation context (immortal).
//        Bar bar = new Bar();
//            ^
//scope/TestEscaping4.java:18: Variables of type Baz are not allowed in this allocation context (immortal).
//        Baz baz = new Baz();
//            ^
//2 errors

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;


@Scope("immortal")
class Foo {

    @DefineScope(name="a", parent="immortal") PrivateMemory a;
    @DefineScope(name="b", parent="immortal") PrivateMemory b;
    
    public void foo () {
        Bar bar = new Bar();
        Baz baz = new Baz();
        
        bar.a = baz.b;
    }
    
}

@Scope("a")
class Bar { 
    public Foo a; 
}
        
@Scope("b") 
class Baz { 
    public Foo b; 
}



/*
@Scope("mission")
public class TestEscaping4 {

    @DefineScope(name="mission", parent="immortal") PrivateMemory mission;
    
    private G1 g;
    
    static class Helper {
        static void foo() {
            ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new @DefineScope(name="a", parent="mission") Rrun1());
        }
        
    }
}

@Scope("mission") @RunsIn("a")
class Rrun1 implements Runnable {
    
    private G1 g;
    
    
    @Override
    public void run() {
        
        // current context is a
        G1 gg = g.field;
    }
}

class G1 {
    
    public G1 field;
    
}
*/