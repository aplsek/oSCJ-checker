package thruster.engine;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

/**
 * This class terminates the mission sequencer once it is released.
 *
 * @author Lilei Zhai
 *
 */
@SCJAllowed(value = LEVEL_1, members = true)
@Scope("ThrusterMission")
@DefineScope(name = "MyTerminatorPeriodicEventHandler", parent = "ThrusterMission")
public class MyTerminatorPeriodicEventHandler extends PeriodicEventHandler {

    @SCJRestricted(INITIALIZATION)
    public MyTerminatorPeriodicEventHandler(PriorityParameters priority,
            PeriodicParameters release, StorageParameters storage,
            long memSize, String name) {
        super(priority, release, storage, name);
    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("MyTerminatorPeriodicEventHandler")
    public void handleAsyncEvent() {
        // System.out.println("TestCase 22: PASS. the terminator PEH of terminator Mission is released.");
        Mission.getCurrentMission().requestSequenceTermination();
    }

    @Override
    public void cleanUp() {
    }

}
