// no error here

package scope;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope("scope.MyDummyMission")
@DefineScope(name="scope.MyDummyMission", parent=IMMORTAL) 
class MyDummyMission extends Mission {

    @Override
    protected void initialize() {
        new TestPEH(null, null, null);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
}


@Scope("scope.MyDummyMission") 
@DefineScope(name="scope.TestPEH", parent="scope.MyDummyMission") 
public class TestPEH  extends PeriodicEventHandler {
    
    public TestPEH(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp) {
        super(priority, parameters, scp);
    }

    @RunsIn("scope.TestPEH")
    public void handleAsyncEvent() {
          ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
    }

    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}