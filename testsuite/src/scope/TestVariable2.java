///Users/plsek/_work/workspace_RT/scj-annotations/tests/scope/TestVariable2.java:28: Cannot assign expression in scope A to variable in scope B.
//            o = a; // Error  
//              ^
//1 error
package scope;


import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;


@Scope(IMMORTAL) 
@DefineScope(name="A", parent=IMMORTAL)
public class TestVariable2 {
    PrivateMemory a;
    PrivateMemory b;
}

@DefineScope(name="A", parent=IMMORTAL)
@Scope("A") 
class A1 {
    void bar() { }
}

@DefineScope(name="B", parent="A")
@Scope("B") 
class B1 {
    A1 a; 
    A1 a2 = new A1(); // Error 
    Object o;

    void foo(A1 a) {
        o = a; // Error  
       // a.bar(); // Error
    }
}