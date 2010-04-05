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
import javax.safetycritical.annotate.ScopeDef;

@Scope("immortal")
public class TestExecuteInArea {
    @ScopeDef(name = "a", parent = "immortal")
    PrivateMemory a = new PrivateMemory();
    @ScopeDef(name = "b", parent = "immortal")
    PrivateMemory b = new PrivateMemory();
    @ScopeDef(name = "c", parent = "a")
    PrivateMemory c = new PrivateMemory();
    GoodR r2 = new GoodR();
    
    public void foo() {
        R r = new R();
        a.executeInArea(r);
    }
    
    @RunsIn("c")
    public void bar() {
        a.executeInArea(r2);
    }
}

@Scope("immortal")
@RunsIn("b")
class R implements Runnable {
    @RunsIn("a")
    public void run() {
        
    }
}

@Scope("immortal")
@RunsIn("a")
class GoodR implements Runnable {
    public void run() {
        
    }
}