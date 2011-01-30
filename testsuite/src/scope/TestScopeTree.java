//scope/TestScopeTree.java:41: Scope Definitions are not consistent: 
//        ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
//                      ^
//        Scope *TestVariable* is not defined but is parent to other scope.
//1 error

package scope;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@DefineScope(name = "TestVariable", parent = IMMORTAL)
class TestVariableClass {}

@Scope(IMMORTAL)
@DefineScope(name = "MyHandler", parent = "TestVariable")
public class TestScopeTree extends PeriodicEventHandler {

    public TestScopeTree(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize) {
        super(priority, parameters, scp);
    }

    @SCJRestricted(maySelfSuspend=true)
    public void handleAsyncEvent() {
        A aObj = new A(); // Error
        B bObj = new B(); // Ok

       
        ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();

        mem.enterPrivateMemory(1000, new
        ARunnable111()); // Error

        mem.enterPrivateMemory(1000, new
        BRunnable111()); // Ok
    }

    @Scope("TestVariable")
    class A {
        void bar() {
        }
    }

    @Scope("MyHandler")
    class B {
        A a;
        A a2 = new A(); // Error
        Object o;

        @SCJRestricted(mayAllocate=false)
        void foo(A a) {
            o = a; // Error
            // a.bar(); // Error
        }
    }

    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }

}

@Scope("MyHandler")
@DefineScope(name="MyMissionInit", parent="MyHandler")
class ARunnable111 implements Runnable {

    @Override
    @RunsIn("MyMissionInit")
    public void run() {
    }
}

@Scope("MyHandler")
@DefineScope(name="BRunnable", parent="MyHandler")
class BRunnable111 implements Runnable {

    @Override
    @RunsIn("BRunnable")
    public void run() {
    }
}
