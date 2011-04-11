package level1;


import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.Mission;
import javax.safetycritical.PriorityScheduler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope("OneMission")
@DefineScope(name="OneMission", parent=IMMORTAL)
@SCJAllowed(value=LEVEL_1, members=true)
public class OneMission extends Mission {

    @SCJRestricted(INITIALIZATION)
    @Override
    public void initialize() {
        new PEH(new PriorityParameters(PriorityScheduler.instance()
                .getNormPriority()), new PeriodicParameters(new RelativeTime(0,
                0), new RelativeTime(500, 0)), new StorageParameters(50000L,
                1000L, 1000L));
    }

    @Override
    @SCJAllowed
    public long missionMemorySize() {
        return 1000L;
    }
}
