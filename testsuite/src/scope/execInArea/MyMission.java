//testsuite/src/scope/execInArea/MyMission.java:62: Runnable and PrivateMemory scopes disagree.
//        mem.executeInArea(runB);                    // ERROR
//                         ^
//1 error

package scope.execInArea;



import javax.realtime.MemoryArea;
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
import static javax.safetycritical.annotate.Scope.UNKNOWN;


@DefineScope(name="MyMission",parent=IMMORTAL)
@Scope("MyMission") 
class MyMission extends Mission {

    protected void initialize() { 
        new MyHandler(null, null, null, 0,this);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
    
}


@Scope("MyMission")
@DefineScope(name="MyHandler",parent="MyMission")
class MyHandler extends PeriodicEventHandler {

    MyMission mission = null;
    
    public MyHandler(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize, MyMission mission) {
        super(priority, parameters, scp);
        
        this.mission = mission;
        
    }

    MyRunnable runA = new MyRunnable();
    
    @Scope(IMMORTAL)  static MyRunnableB runB = new MyRunnableB();
    
    @RunsIn("MyHandler") 
    public void handleAsyncEvent() {
        @Scope("MyMission") MemoryArea mem = MemoryArea.getMemoryArea(mission);   // OK
        // TODO: the getMemoryArea needs to be properly checked!!!!
        
        mem.executeInArea(runA);                    // OK
        
        mem.executeInArea(runB);                    // ERROR
        
        
        ManagedMemory manMem = ManagedMemory.getCurrentManagedMemory();
        manMem.executeInArea(runA);
        
    }

    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}



@Scope("MyMission") 
class MyRunnable implements Runnable {
    @Override
    @RunsIn("MyMission") 
    public void run() {
    } 
}


@Scope(IMMORTAL) 
class MyRunnableB implements Runnable {
    @Override
    @RunsIn(IMMORTAL) 
    public void run() {
    } 
}