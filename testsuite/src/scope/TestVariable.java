//scope/TestVariable.java:52: Cannot assign expression in scope scope.TestVariable to variable in scope scope.DummyMission.
//          A aObj = new A(); // Error 
//            ^
//scope/TestVariable.java:52: Object allocation in a context (scope.TestVariable) other than its designated scope (scope.DummyMission).
//          A aObj = new A(); // Error 
//                   ^
//scope/TestVariable.java:87: Cannot assign expression in scope scope.DummyMission to variable in scope scope.TestVariable.
//               o = a; // Error  
//                 ^
//3 errors

package scope;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope("scope.DummyMission") 
@DefineScope(name="scope.DummyMission",parent=IMMORTAL)
class DummyMission extends Mission {

    @Override
    protected void initialize() {
        new TestVariable(null, null, null, 0);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

}

@Scope("scope.DummyMission") 
@DefineScope(name="scope.TestVariable", parent="scope.DummyMission") 
public class TestVariable  extends PeriodicEventHandler {

    public TestVariable(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize) {
        super(priority, parameters, scp);
    }

    @RunsIn("scope.TestVariable")
    public
    void handleAsyncEvent() {
        A aObj = new A(); // Error 
        B bObj = new B(); // Ok 


        ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();

        mem.enterPrivateMemory(1000, new 
                ARunnable()); 

        mem.enterPrivateMemory(1000, new 
                BRunnable()); // Ok
    }
    
    @Scope("scope.DummyMission") 
    class A {
        void bar() { }
    }

    @Scope("scope.TestVariable") 
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



    @Scope("scope.TestVariable") 
    @DefineScope(name="ARunnable",parent="scope.TestVariable")
    class ARunnable implements Runnable {
        @Override
        @RunsIn("ARunnable") 
        public void run() {
        }  
    }

    @Scope("scope.TestVariable") 
    @DefineScope(name="BRunnable",parent="scope.TestVariable")
    class BRunnable implements Runnable {

        @Override
        @RunsIn("BRunnable") 
        public void run() {
        }
    }
}