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

import static javax.safetycritical.annotate.Restrict.MAY_ALLOCATE;
import static javax.safetycritical.annotate.Restrict.MAY_BLOCK;
import static javax.safetycritical.annotate.Restrict.ALLOCATE_FREE;

@Scope("immortal")
public class TestScopeTree extends PeriodicEventHandler {

    //@DefineScope(name="TestVariable", parent="immortal") <-------- Errrrrrrrr
    // ManagedMemory mem1 = ManagedMemory.getCurrentManagedMemory();

    public TestScopeTree(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize) {
        super(priority, parameters, scp, memSize);
        // TODO Auto-generated constructor stub
    }

    // /@SCJRestricted({MAY_ALLOCATE,MAY_BLOCK})
    public void handleEvent() {
        A aObj = new A(); // Error
        B bObj = new B(); // Ok

        @DefineScope(name = "MyHandler", parent = "TestVariable")
        ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();

        mem.enterPrivateMemory(1000, new
        /*@DefineScope(name="MyMissionInit", parent="MyHandler")*/
        ARunnable111()); // Error

        mem.enterPrivateMemory(1000, new
        /*@DefineScope(name="BRunnable", parent="MyHandler")*/
        BRunnable111()); // Ok
    }

    @Override
    public void handleAsyncEvent() {
        // TODO Auto-generated method stub

    }

    @Scope("TestVariable")
    class A {
        // /@SCJRestricted({MAY_ALLOCATE})
        void bar() {
        }
    }

    @Scope("MyHandler")
    class B {
        A a;
        A a2 = new A(); // Error
        Object o;

        // /@SCJRestricted({ALLOCATE_FREE})
        void foo(A a) {
            o = a; // Error
            // a.bar(); // Error
        }
    }

    @Override
    public void register() {
        // TODO Auto-generated method stub

    }

    @Override
    public StorageParameters getThreadConfigurationParameters() {
        // TODO Auto-generated method stub
        return null;
    }

}

@Scope("MyHandler")
@RunsIn("MyMissionInit")
class ARunnable111 implements Runnable {

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }
}

@Scope("MyHandler")
@RunsIn("BRunnable")
class BRunnable111 implements Runnable {

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }
}
