// NO ERROR

package scope.unknown;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

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
public class UnknownMethod extends Mission {

    
    protected void initialize() { 
        new MyHandler(null, null, null, 0,this);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
    
    Foo foo;
    
    @Scope(UNKNOWN) @RunsIn(UNKNOWN)
    public Foo unknownMethod() {
        return foo;
    }
    
}


@Scope("MyMission")
@DefineScope(name="MyHandler",parent="MyMission")
class MyHandler extends PeriodicEventHandler {

    UnknownMethod mission = null;
    
    public MyHandler(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize, UnknownMethod mission) {
        super(priority, parameters, scp);
        
        //this.mission = mission;
        
    }

    @RunsIn("MyHandler") 
    public void handleAsyncEvent() {
        mission.unknownMethod();
    }

    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}

class Foo {
}
