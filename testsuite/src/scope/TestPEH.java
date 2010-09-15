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


class MyDummyMission extends Mission {

    @Override
    protected void initialize() {
        new TestVariable(null, null, null, 0);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
     
}


@Scope("scope.MyDummyMission") 
@RunsIn("scope.TestPEH")
public class TestPEH  extends PeriodicEventHandler {
    
    public TestPEH(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize) {
        super(priority, parameters, scp, memSize);
    }

    public
    void handleEvent() {
          @DefineScope(name="scope.TestPEH", parent="scope.MyDummyMission") 
          ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
    }

    @Override
    public void handleAsyncEvent() {
    }
      

    @Override
    public void register() {
    }

    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}