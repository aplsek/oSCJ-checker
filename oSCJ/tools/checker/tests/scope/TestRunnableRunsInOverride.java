//scope/TestRunnableRunsInOverride.java:14: @RunsIn annotations must agree with their overridden annotations.
//    public void run() {
//                ^
//1 error

package scope;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("a")
@RunsIn("a")
public class TestRunnableRunsInOverride implements Runnable {
    @ScopeDef(name="a", parent="immortal") PrivateMemory a;
    @ScopeDef(name="b", parent="a") PrivateMemory b;
    @RunsIn("b")
    public void run() {
        
    }
}
