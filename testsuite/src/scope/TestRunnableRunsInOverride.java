//scope/TestRunnableRunsInOverride.java:19: Methods must run in the same scope or a child scope of their owning type.
//    public void run() {
//                ^
//scope/TestRunnableRunsInOverride.java:36: The Runnable's @RunsIn must be a child scope of the CurrentScope
//                    enterPrivateMemory(0, new R2());
//                                      ^
//         @RunsIn: b 
//         Current Scope: a
//scope/TestRunnableRunsInOverride.java:44: Methods must run in the same scope or a child scope of their owning type.
//            public void run() {
//                        ^
//3 errors

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope("a")
@DefineScope(name = "a", parent = IMMORTAL)
public class TestRunnableRunsInOverride implements Runnable {
    @RunsIn("b")
    public void run() {
        
    }
    
    static class Helper {
        static void foo() {
            ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new R1());
        }
        @Scope(IMMORTAL)
        static class R1 implements Runnable {
            @Override
            @RunsIn("a")
            public void run() {
                ManagedMemory.
                getCurrentManagedMemory().
                    enterPrivateMemory(0, new R2());
            }
        }
        @Scope("a")
        @DefineScope(name = "b", parent = IMMORTAL)
        static class R2 implements Runnable {
            @Override
            @RunsIn("b")
            public void run() {
            }
        }
    }
}
