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
import static javax.safetycritical.annotate.Scope.IMMORTAL;


@DefineScope(name="scope.MyMission",parent=IMMORTAL)
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
        //                ARunnable1()); // Ok
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

}


@Scope("scope.MyMission")  
@RunsIn("scope.MyHandler") 
@DefineScope(name="scope.MyHandler",parent="scope.MyMission")
class MyHandler extends PeriodicEventHandler {

    public MyHandler(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize) {
        super(priority, parameters, scp);
    }

    public
    void handleAsyncEvent() {
        
        A aObj = new A();                                                
        B bObj = new B(); // Ok 

       
        ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();

        //ARunnable1 aRunnable = new ARunnable1();
        //mem.enterPrivateMemory(1000, aRunnable);                         // ERROR

        ARunnable1 aRunnable = new ARunnable1();
        ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, aRunnable);    
        
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
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }

}



@Scope("scope.MyHandler") 
@DefineScope(name="scope.MyHandler", parent="scope.MyMission")
class BRunnable1 implements Runnable {
    @Override
    @RunsIn("MyMissionRunB") 
    public void run() {
    } 
}


@Scope("scope.MyMission") 

@DefineScope(name="MyMissionInitA", parent="scope.MyMission")
class ARunnable1 implements Runnable {
    @Override
    @RunsIn("MyMissionInitA") 
    public void run() {
    }  
}

class RunnableNull implements Runnable {
    @Override
    public void run() {
    } 
}


@Scope("scope.MyHandler") 
@DefineScope(name="MyMissionRunB", parent="scope.MyHandler") 
class CRunner implements Runnable {
    @Override
    @RunsIn("MyMissionRunB") 
    public void run() {
    }   
}

