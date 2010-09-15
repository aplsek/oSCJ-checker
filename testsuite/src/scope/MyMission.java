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



@Scope("scope.MyMission") 
class MyMission extends Mission {

    protected
    void initialize() { 
        new MyHandler(null, null, null, 0);
        
        //@DefineScope(name="MyMissionInitA", parent="scope.MyMission")
        //ARunnable1 aRunnable = new ARunnable1();
        //ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, aRunnable);    
        
        //ManagedMemory.getCurrentManagedMemory().
        //    enterPrivateMemory(1000, new 
        //            /*@DefineScope(name="MyMissionInitA", parent="scope.MyMission")*/ 
        //                ARunnable1()); // Ok
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
        
        A aObj = new A();                                                
        B bObj = new B(); // Ok 

        @DefineScope(name="scope.MyHandler", parent="scope.MyMission") 
        ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();

        //ARunnable1 aRunnable = new ARunnable1();
        //mem.enterPrivateMemory(1000, aRunnable);                         // ERROR

        @DefineScope(name="MyMissionInitA", parent="scope.MyMission")
        ARunnable1 aRunnable = new ARunnable1();
        ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, aRunnable);    
        
        
        @DefineScope(name="MyMissionRunB", parent="scope.MyHandler") 
        BRunnable1 bRunnable = new BRunnable1();
        mem.enterPrivateMemory(2000, bRunnable);                          // Ok
        
        CRunner cRun = new CRunner();
        mem.enterPrivateMemory(2000, cRun);
        
        RunnableNull runNull = new RunnableNull();
        mem.enterPrivateMemory(2000, runNull);
        
    }


    class A {
        void bar() { }
    }

    class B {
        A a; 
        A a2 = new A();                     
        Object o;

        @SCJRestricted(mayAllocate=false)
        void foo(A a) {
            o = a;                          
            //a.bar();                     // ERROR  (not reported since its commented out
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



@Scope("scope.MyHandler") 
@RunsIn("MyMissionRunB") 
class BRunnable1 implements Runnable {
    @Override
    public void run() {
    } 
}


@Scope("scope.MyMission") 
@RunsIn("MyMissionInitA") 
class ARunnable1 implements Runnable {
    @Override
    public void run() {
    }  
}

class RunnableNull implements Runnable {
    @Override
    public void run() {
    } 
}


@Scope("scope.MyHandler") 
@RunsIn("MyMissionRunB") 
class CRunner implements Runnable {
    @Override
    public void run() {
    }   
}

