///Users/plsek/_work/workspace_RT/scj-annotations/tests/scope/TestVariable2.java:28: Cannot assign expression in scope A to variable in scope B.
//            o = a; // Error  
//              ^
//1 error
package scope;

import static javax.safetycritical.annotate.Restrict.ALLOCATE_FREE;
import static javax.safetycritical.annotate.Restrict.MAY_ALLOCATE;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope("immortal") 
public class TestVariable2 {
    @DefineScope(name="A", parent="immortal") PrivateMemory a;
    @DefineScope(name="B", parent="A") PrivateMemory b;
    
    

    
}


@Scope("A") 
class A1 {
    void bar() { }
}

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