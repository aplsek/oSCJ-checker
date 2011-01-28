//scope/TestExecuteInArea.java:17: Runnable and PrivateMemory scopes disagree.
//        a.executeInArea(r);
//                       ^
//scope/TestExecuteInArea.java:25: @RunsIn annotations must agree with their overridden annotations.
//    public void run() {
//                ^
//2 errors

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope(IMMORTAL)
@DefineScope(name = "a", parent = IMMORTAL)
public class TestExecuteInArea {
    PrivateMemory a = new PrivateMemory(0);
    PrivateMemory b = new PrivateMemory(0);
    PrivateMemory c = new PrivateMemory(0);
    
    GoodR r2 = new GoodR();
    
    public void foo() {
        R111 r = new R111();
        a.executeInArea(r);
    }
    
    @RunsIn("c")
    public void bar() {
        a.executeInArea(r2);
    }
}

@Scope(IMMORTAL)
@RunsIn("b")
@DefineScope(name = "b", parent = IMMORTAL)
class R111 implements Runnable {
    @RunsIn("a")
    public void run() {    // FAIL:  @RunsIn annotations must agree with their overridden annotations.
    }
}

@Scope(IMMORTAL)
@RunsIn("a")
@DefineScope(name = "c", parent = "a")
class GoodR implements Runnable {
    public void run() {
    }
}