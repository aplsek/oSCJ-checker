//scope/TestRunsIn.java:16: Methods must run in the same scope or a child scope of their owning type.
//    public void foo() {
//                ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("b")
public class TestRunsIn {
    @RunsIn("a")
    public void foo() {
        
    }
    static class Helper {
        static void foo() {
            ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new /*@DefineScope(name="a", parent="immortal")*/ R1());
        }
//        @Scope("immortal") @RunsIn("a")
//        static class R1 implements Runnable {
//            @Override
//            public void run() {
//                ManagedMemory.enterPrivateMemory(0, new /*@DefineScope(name="b", parent="a")*/ R2());
//            }
//        }
//        @Scope("a") @RunsIn("b")
//        static class R2 implements Runnable {
//            @Override
//            public void run() {
//            }
//        }
    }
}

@Scope("immortal") @RunsIn("a")
class R1 implements Runnable {
    @Override
    public void run() {
        ManagedMemory.
        getCurrentManagedMemory().
            enterPrivateMemory(0, new /*@DefineScope(name="b", parent="a")*/ R2());
    }
}
@Scope("a") @RunsIn("b")
class R2 implements Runnable {
    @Override
    public void run() {
    }
}