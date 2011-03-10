package scope.level2;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PriorityScheduler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true, value=LEVEL_2)
@Scope("PrimaryMission")
@DefineScope(name="PrimaryMission", parent=IMMORTAL)
public class PrimaryMission extends Mission {
    final int MISSION_MEMORY_SIZE = 10000;

    static final private int PRIORITY = PriorityScheduler.instance()
            .getNormPriority();

    @Override
    @SCJRestricted(INITIALIZATION)
    public void initialize() {
        ManagedMemory.getCurrentManagedMemory().resize(MISSION_MEMORY_SIZE);
        PriorityParameters pp = new PriorityParameters(PRIORITY);
        StorageParameters sp = new StorageParameters(100000L, 1000, 1000);
        SubMissionSequencer sms = new SubMissionSequencer(pp, sp);
        // sms.register();
        (new MyPeriodicEventHandler("AEH A", new RelativeTime(0, 0),
                new RelativeTime(500, 0))).register();

        (new MyPeriodicEventHandler("AEH B", new RelativeTime(0, 0),
                new RelativeTime(1000, 0))).register();
        (new MyPeriodicEventHandler("AEH C", new RelativeTime(500, 0),
                new RelativeTime(500, 0))).register();

    }

    @Override
    public long missionMemorySize() {
        return MISSION_MEMORY_SIZE;
    }

}
