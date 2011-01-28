//scope/TestEscaping4.java:17: Variables of type Bar are not allowed in this allocation context (IMMORTAL).
//        Bar bar = new Bar();
//            ^
//scope/TestEscaping4.java:18: Variables of type Baz are not allowed in this allocation context (IMMORTAL).
//        Baz baz = new Baz();
//            ^
//2 errors

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope(IMMORTAL)
class Foo {

    PrivateMemory a;
    PrivateMemory b;
    
    public void foo () {
        Bar bar = new Bar();
        Baz baz = new Baz();
        
        bar.a = baz.b;
    }
    
}

@Scope("a")
@DefineScope(name="a", parent=IMMORTAL)
class Bar { 
    public Foo a; 
}
        
@Scope("b")
@DefineScope(name="b", parent=IMMORTAL)
class Baz { 
    public Foo b; 
}