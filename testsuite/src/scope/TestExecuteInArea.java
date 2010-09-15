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

@Scope("immortal")
public class TestExecuteInArea {
    @DefineScope(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory(0);
    
    @DefineScope(name = "b", parent = "immortal")
    PrivateMemory b = new PrivateMemory(0);
    
    @DefineScope(name = "c", parent = "a")
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

@Scope("immortal")
@RunsIn("b")
class R111 implements Runnable {
    @RunsIn("a")
    public void run() {    // FAIL:  @RunsIn annotations must agree with their overridden annotations.
    }
}

@Scope("immortal")
@RunsIn("a")
class GoodR implements Runnable {
    public void run() {
    }
}