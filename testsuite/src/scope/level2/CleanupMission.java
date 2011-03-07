package scope.level2;

import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PriorityScheduler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true, value = LEVEL_2)
@Scope("CleanupMission")
@DefineScope(name = "CleanupMission", parent = Scope.IMMORTAL)
public class CleanupMission extends Mission {
    static final private int MISSION_MEMORY_SIZE = 10000;
    static final private int PRIORITY = PriorityScheduler.instance()
            .getNormPriority();

    @Override
    @SCJRestricted(INITIALIZATION)
    public void initialize() {
        ManagedMemory.getCurrentManagedMemory().resize(MISSION_MEMORY_SIZE);
        PriorityParameters pp = new PriorityParameters(PRIORITY);
        StorageParameters sp = new StorageParameters(100000L, 1000, 1000);
        MyCleanupThread t = new MyCleanupThread(pp, sp);
    }

    @Override
    public long missionMemorySize() {
        return MISSION_MEMORY_SIZE;
    }
}