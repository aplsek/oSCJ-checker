package thruster.engine;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.realtime.PriorityParameters;
import javax.realtime.PriorityScheduler;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;


/**
 */
@SCJAllowed(value = LEVEL_1, members = true)
@Scope(IMMORTAL)
@DefineScope(name = "ThrusterMission", parent = IMMORTAL)
public class ThrusterControlSequencer extends MissionSequencer {

    private static final int NORM_PRIORITY = PriorityScheduler.instance()
            .getNormPriority();

    // singleton pattern
    private static ThrusterControlSequencer thrusterControlSequencer;

    private static final int NO_MISSION = 0;
    private static final int NORMAL_MISSION = 1;
    private static final int TERMINATOR_MISSION = 2;
    private static final int DUMMY_MISSION = 3;
    private static int curMissionNum = NO_MISSION;

    @SCJRestricted(INITIALIZATION)
    private ThrusterControlSequencer(PriorityParameters priority,
            StorageParameters storage) {
        super(priority, storage);
        // System.out.println("Sequencer created");
    }

    @Scope(IMMORTAL) @RunsIn(IMMORTAL)
    public static MissionSequencer getInstance() {
        // System.out.println("Thruster getInstance called");
        if (thrusterControlSequencer == null) {
            PriorityParameters myPriorityPar = new PriorityParameters(
                    NORM_PRIORITY);
            StorageParameters myStoragePar = new StorageParameters(100000L,
                    1000, 1000);

            thrusterControlSequencer = new ThrusterControlSequencer(
                    myPriorityPar, myStoragePar);
            // System.out.println("Sequencer created");
        }

        // return null;
        return thrusterControlSequencer;
    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("ThrusterMission")
    @Scope("ThrusterMission")
    protected Mission getNextMission() {
        /*
         * Use boolean instead of MyMission reference here, because Immortal
         * can't refer to Scoped
         */
        // System.out.println("TestCase 03: PASS. MissionSequencer.getNextMission() is executed.");
        switch (curMissionNum++) {
        case NO_MISSION:
            return new ThrusterMission();
        case NORMAL_MISSION:
            return new MyTerminatorMission();
        case TERMINATOR_MISSION:
            return new MyDummyMission();
        case DUMMY_MISSION:
            return null;
        default:
            // System.err.println("Error: invalid curMissionNum: "+curMissionNum);
            return null;
        }
    }

}
