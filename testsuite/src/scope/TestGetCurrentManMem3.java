//scope/TestGetCurrentManMem3.java:69: The @DefineScope variable is not consistent with its scope definition, check the parent of the scope!
//          ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
//                        ^
//         Allocation-Context: scope.DummyMission2 
//        DefineScope name: scope.TestGetCurrentManMem3
//1 error

package scope;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;


@Scope("scope.DummyMission2")
@DefineScope(name="scope.DummyMission2",parent=IMMORTAL)
class DummyMission2 extends CyclicExecutive {

    public DummyMission2(StorageParameters storage) {
        super(storage);
    }

    @Override
    protected void initialize() {
        new TestGetCurrentManMem3(null, null, null);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    @Override
    public CyclicSchedule getSchedule(PeriodicEventHandler[] peh) {
        return null;
    }

    @Override
    public void setUp() {
    }

    @Override
    public void tearDown() {
    }
     
}

@Scope("scope.DummyMission2")
@RunsIn("scope.TestGetCurrentManMem3")
@DefineScope(name="scope.TestGetCurrentManMem3",parent="scope.DummyMission2")
public class TestGetCurrentManMem3 extends PeriodicEventHandler  {
    
    PrivateMemory mem;
    
    public TestGetCurrentManMem3(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp) {
        super(priority, parameters, scp);
    }

    @RunsIn("scope.TestGetCurrentManMem3")
    public
    void handleAsyncEvent() {
          ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
    }
    
    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}

