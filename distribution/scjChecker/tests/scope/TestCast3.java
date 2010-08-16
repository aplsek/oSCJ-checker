//scope/TestCast3.java:27: Variables of type Bcast are not allowed in this allocation context (immortal).
//    void bar(Bcast b1) {
//                   ^
//scope/TestCast3.java:28: Cannot assign expression in scope b to variable in scope immortal.
//        Acast a = (Bcast) b1; // ERROR
//              ^
//scope/TestCast3.java:31: Variables of type Bcast are not allowed in this allocation context (immortal).
//    void bar2(Bcast b1) {
//                    ^
//scope/TestCast3.java:32: Cannot assign expression in scope b to variable in scope immortal.
//        Acast a = b1; // ERROR
//              ^
//4 errors

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;



class Acast {
}

@Scope("immortal")
public class TestCast3 {

    @DefineScope(name = "b", parent = "immortal")
    PrivateMemory b = new PrivateMemory(0);
    
    void bar(Bcast b1) {
        Acast a = (Bcast) b1; // ERROR
    }
    
    void bar2(Bcast b1) {
        Acast a = b1; // ERROR
    }

}




@Scope("b")
class Bcast extends Acast {
}

@Scope("immortal")
@RunsIn("b")
class RunnableCast implements Runnable {

    @Override
    public void run() {
    }
    
    @RunsIn("b")
    void foo(Bcast b) {
        Acast a = b; // valid
    }
    
}