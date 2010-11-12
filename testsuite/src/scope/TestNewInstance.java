//scope/TestNewInstance.java:21: Cannot allocate objects of type scope.NewInstance inside scope a.
//        a.newInstance(NewInstance.class);
//                     ^
//1 error

package scope;

import javax.realtime.ImmortalMemory;
import javax.realtime.InaccessibleAreaException;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("immortal")
class NewInstance {
}

@Scope("a")
public class TestNewInstance {
    public void test() throws IllegalArgumentException,  IllegalAccessException, OutOfMemoryError, InaccessibleAreaException, InstantiationException {
        ImmortalMemory.instance().newInstance(NewInstance.class);
       
        ManagedMemory.getCurrentManagedMemory().newInstance(NewInstance.class);
    }
    
    static class Helper {
        static void foo() {
            ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new /*@DefineScope(name="a", parent="immortal")*/ R1());
        }
        @Scope("immortal") @RunsIn("a")
        static class R1 implements Runnable {
            @Override
            public void run() {
            }
        }
    }
}