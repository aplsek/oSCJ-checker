//scope/TestCast.java:35: (Class scope.A11 has a disagreeing @Scope annotation from parent class scope.B11)
//class B11 extends A11 {
//^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

public class TestCast {

    PrivateMemory a = new PrivateMemory(0);
    PrivateMemory b = new PrivateMemory(0);
}

@Scope("a")
@DefineScope(name = "a", parent = IMMORTAL)
class A11  {
}

@Scope("b")                 // ERROR
@DefineScope(name = "b", parent = "a")
class B11 extends A11 {  
}

@Scope("a")
class C1 extends A11 {
}



@Scope(IMMORTAL) 
class Runnable1 implements Runnable {
    @Override
    @RunsIn("a")
    public void run() {
        A11 a = new A11();
        B11 b = (B11) a;   // ERROR but the previous error gets detected first.
        C1  c = (C1) a;   // OK
    }
}