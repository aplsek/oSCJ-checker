//scope2/MyMission2.java:55: The Runnable passed into the enterPrivateMemory() call must have a run() method with a @RunsIn annotation.
//        ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, bRunnable111); 
//                                                                  ^
//scope2/MyMission2.java:58: The Runnable class must have a matching @Scope annotation.
//        ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, runnableMission); 
//                                                                  ^
//scope2/MyMission2.java:66: The Runnable's @RunsIn must be a child scope of the CurrentScope
//        ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, wrongScopeRun); 
//                                                                  ^
//         @RunsIn: wrong_scope 
//         Current Scope: MyHandler2
//scope2/MyMission2.java:105: Methods must run in the same scope or a child scope of their owning type.
//    public void run() {
//                ^
//4 errors


package scope;


import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@DefineScope(name="MyMission2",parent=IMMORTAL)
@Scope("MyMission2") 
class MyMission2 extends Mission {
    
    protected
    void initialize() { 
        new MyHandler2(null, null, null, 0);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
}


@Scope("MyMission2")  
@DefineScope(name="MyHandler2",parent="MyMission2")
class MyHandler2 extends PeriodicEventHandler {

    public MyHandler2(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize) {
        super(priority, parameters, scp);
    }
    
    BRunnableMission runnableMission = new BRunnableMission();
    
    @RunsIn("MyHandler2") 
    public void handleAsyncEvent() {

        ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
        BRunnable12 bRunnable = new BRunnable12();
        ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, bRunnable);  
        
        BRunnable112 bRunnable112 = new BRunnable112();
        ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, bRunnable112); 
        
        ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, runnableMission); 
        
        WrongScopeRunnable wrongScopeRun = new WrongScopeRunnable();
        ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, wrongScopeRun); 
        
    }

    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }



   

}


@Scope("MyHandler2") 
@DefineScope(name="BRunnable12", parent="MyHandler2") 
class BRunnable12 implements Runnable {
    
    @RunsIn("BRunnable12") 
    public void run() {
    } 
}


@Scope("MyMission2")                                    // ERR
class BRunnableMission implements Runnable {
    
    @RunsIn("BRunnable12") 
    public void run() {
    } 
}

@Scope("MyHandler2")      
@DefineScope(name="wrong_scope", parent="MyMission2") 
class WrongScopeRunnable implements Runnable {
    
    @RunsIn("wrong_scope")              // ERR
    public void run() {
    } 
}


@Scope("MyHandler2") 
@DefineScope(name="BRunnable112", parent="MyHandler2") 
class BRunnable112 implements Runnable {
    
    public void run() {                                 // ERR: no @RunsIn
    } 
}