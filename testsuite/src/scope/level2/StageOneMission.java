package scope.level2;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.RelativeTime;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true, value=LEVEL_2)
@Scope("StageOneMission")
@DefineScope(name="StageOneMission", parent=IMMORTAL)
public class StageOneMission extends Mission {
    private static final int MISSION_MEMORY_SIZE = 10000;

    @Override
    @SCJRestricted(INITIALIZATION)
    public void initialize() {
        ManagedMemory.getCurrentManagedMemory().resize(MISSION_MEMORY_SIZE);
        (new MyPeriodicEventHandler("stage1.eh1", new RelativeTime(0, 0),
                new RelativeTime(1000, 0))).register();
    }

    @Override
    @SCJAllowed
    public long missionMemorySize() {
        return MISSION_MEMORY_SIZE;
    }
}
