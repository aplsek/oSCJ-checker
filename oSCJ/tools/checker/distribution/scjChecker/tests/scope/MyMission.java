//scope/MyMission.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        mem.enterPrivateMemory(1000, new 
//                                     ^
//1 error

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


import static javax.safetycritical.annotate.Restrict.MAY_ALLOCATE;
import static javax.safetycritical.annotate.Restrict.MAY_BLOCK;
import static javax.safetycritical.annotate.Restrict.ALLOCATE_FREE;
import static javax.safetycritical.annotate.Restrict.INITIALIZATION;




@Scope("scope.MyMission") 
class MyMission extends Mission {

    protected
    void initialize() { 
        new MyHandler(null, null, null, 0);
        
        ManagedMemory.getCurrentManagedMemory().
            enterPrivateMemory(1000, new 
                    /*@DefineScope(name="MyMissionInitA", parent="scope.MyMission")*/ 
                        ARunnable1()); // Ok

    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

}


@Scope("scope.MyMission")  
@RunsIn("scope.MyHandler") 
class MyHandler  extends PeriodicEventHandler {

    public MyHandler(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize) {
        super(priority, parameters, scp, memSize);
    }

    public
    void handleEvent() {
        A aObj = new A(); // Error 
        B bObj = new B(); // Ok 

        @DefineScope(name="scope.MyHandler", parent="scope.MyMission") 
        ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();

        mem.enterPrivateMemory(1000, new 
                /*@DefineScope(name="MyMissionRunA", parent="scope.MyHandler")*/
                ARunnable1()); // Error

        mem.enterPrivateMemory(1000, new 
                /*@DefineScope(name="MyMissionRunB", parent="scope.MyHandler")*/ 
                BRunnable1()); // Ok
    }


    class A {
        @SCJRestricted({MAY_ALLOCATE}) 
        void bar() { }
    }

    class B {
        A a; 
        A a2 = new A(); // Error 
        Object o;

        @SCJRestricted({ALLOCATE_FREE}) 
        void foo(A a) {
            o = a; // Error  
            // a.bar(); // Error
        }
    }

    @Override
    public void register() {
    }

    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }


}

@Scope("scope.MyMission") 
@RunsIn("MyMissionInitA") 
class ARunnable1 implements Runnable {
    @Override
    public void run() {
    }  
}

@Scope("scope.MyHandler") 
@RunsIn("MyMissionRunB") 
class BRunnable1 implements Runnable {
    @Override
    public void run() {
    } 
}
