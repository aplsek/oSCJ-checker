package thruster;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.PriorityScheduler;
import javax.realtime.RelativeTime;
import javax.safetycritical.Mission;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

/**
 * This class contains an PEH which will terminate the MissionSequencer.
 *
 * @author Lilei Zhai
 *
 */
@SCJAllowed(value = LEVEL_1, members = true)
public class MyTerminatorMission extends Mission {
    private MyTerminatorPeriodicEventHandler myPEH;

    @Override
    @SCJRestricted(INITIALIZATION)
    protected void initialize() {

        myPEH = new MyTerminatorPeriodicEventHandler(new PriorityParameters(
                PriorityScheduler.instance().getNormPriority()),
                new PeriodicParameters(new RelativeTime(), new RelativeTime(
                        500, 0)), new StorageParameters(100, 100, 100), 10000,
                "MyPEH");

        myPEH.register();
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    protected void cleanup() {
    }

}
