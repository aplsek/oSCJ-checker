//scope/TestCast3.java:27: Variables of type Bcast are not allowed in this allocation context (IMMORTAL).
//    void bar(Bcast b1) {
//                   ^
//scope/TestCast3.java:28: Cannot assign expression in scope b to variable in scope IMMORTAL.
//        Acast a = (Bcast) b1; // ERROR
//              ^
//scope/TestCast3.java:31: Variables of type Bcast are not allowed in this allocation context (IMMORTAL).
//    void bar2(Bcast b1) {
//                    ^
//scope/TestCast3.java:32: Cannot assign expression in scope b to variable in scope IMMORTAL.
//        Acast a = b1; // ERROR
//              ^
//4 errors

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

class Acast {
}

@Scope(IMMORTAL)
public class TestCast3 {

    @DefineScope(name = "b", parent = IMMORTAL)
    PrivateMemory b = new PrivateMemory(0);
    
    void bar(Bcast b1) {
        Acast a = (Bcast) b1; // ERROR
    }
    
    void bar2(Bcast b1) {
        Acast a = b1; // ERROR
    }
}


@Scope("b")
 @DefineScope(name = "b", parent = IMMORTAL)
class Bcast extends Acast {
}

@Scope(IMMORTAL)
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