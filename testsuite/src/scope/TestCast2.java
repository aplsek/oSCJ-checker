//tests/scope/TestCast2.java:54: Variables of type B111 are not allowed in this allocation context (IMMORTAL).
//    public void foo(B111 b) {
//                         ^
//scope/TestCast2.java:55: Class Cast Error : The class being casted must have a scope (@Scope=b) that is the same as the scope of the target class (@Scope=IMMORTAL).
//        A111 a = (A111) b;              // ERROR
//                 ^
//scope/TestCast2.java:58: Variables of type B111 are not allowed in this allocation context (IMMORTAL).
//    public void foo2(B111 b) {
//                          ^
//scope/TestCast2.java:59: Cannot assign expression in scope b to variable in scope IMMORTAL.
//        A111 a = b;              // ERROR
//             ^
//4 errors

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

public class TestCast2 {

    PrivateMemory a = new PrivateMemory(0);
    PrivateMemory b = new PrivateMemory(0);
}

class A111  {
}

@Scope("b")  
@DefineScope(name = "b", parent = "a")
class B111 extends A111 {  
}

@Scope("a")  
@DefineScope(name = "a", parent = IMMORTAL)
class C11 extends A111 {
}


@Scope(IMMORTAL)
class CastRunnable2 implements Runnable {
    @Override
    @RunsIn("a")
    public void run() {
    }
    
    public void foo(B111 b) {
        A111 a = (A111) b;              // ERROR
    }
    
    public void foo2(B111 b) {
        A111 a = b;              // ERROR
    }
}

