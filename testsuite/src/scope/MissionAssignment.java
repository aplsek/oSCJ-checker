package scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;



@DefineScope(name="MyMission",parent=IMMORTAL)
@Scope("MyMission") 
public class MissionAssignment  extends Mission {
    protected void initialize() { 
        new MyPEH(null, null, null, 0,this);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
    
}



@Scope("MyMission")
@DefineScope(name="MyHandler",parent="MyMission")
class MyPEH extends PeriodicEventHandler {

    MissionAssignment mission = null;
    
    public MyPEH(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize, MissionAssignment mission) {
        super(priority, parameters, scp);
        
        this.mission = mission;    // OK
        
    }

    @RunsIn("MyHandler") 
    public void handleAsyncEvent() {
    }

    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}