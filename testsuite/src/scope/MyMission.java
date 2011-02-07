//scope/MyMission.java:60: Cannot assign expression in scope scope.MyHandler to variable in scope scope.MyMission.
//        ARunnable1 aRunnable = new ARunnable1();
//                   ^
//scope/MyMission.java:60: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        ARunnable1 aRunnable = new ARunnable1();
//                               ^
//scope/MyMission.java:65: The Runnable class must have a matching @Scope annotation.
//        ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, aRunnable);  // ERROR  
//                                                                  ^
//scope/MyMission.java:70: The Runnable passed into the enterPrivateMemory() call must have a run() method with a @RunsIn annotation.
//        mem.enterPrivateMemory(2000, runNull);
//                              ^
//
//4 errors

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

    protected void initialize() { 
        new MyHandler(null, null, null, 0);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

}


@Scope("scope.MyMission")
@DefineScope(name="scope.MyHandler",parent="scope.MyMission")
class MyHandler extends PeriodicEventHandler {

    public MyHandler(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize) {
        super(priority, parameters, scp);
    }

    @RunsIn("scope.MyHandler") 
    public void handleAsyncEvent() {
        
        A aObj = new A();                                                
        B bObj = new B(); // OK
       
        ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();

        ARunnable1 aRunnable = new ARunnable1();                // ERROR
        ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, aRunnable);  // ERROR  
        
        BRunnable1 bRunnable = new BRunnable1();
        mem.enterPrivateMemory(2000, bRunnable);                          // OK
        
        Runnable cc = (Runnable) new CRunner();
        
        CRunner cRun = new CRunner();
        mem.enterPrivateMemory(2000, (Runnable) cRun);                 // OK
        
        RunnableNull runNull = new RunnableNull();
        mem.enterPrivateMemory(2000, runNull);                  // ERROR, no @RunsIn
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

