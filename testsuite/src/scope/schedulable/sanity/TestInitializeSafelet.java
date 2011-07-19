package scope.schedulable.sanity;


import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@SCJAllowed(members=true)
@DefineScope(name="CDMission", parent=IMMORTAL)
public class TestInitializeSafelet extends CyclicExecutive {

    static int priorityParameter = 18;
    static long totalBackingStore = 1000L;
    static int nativeStackSize = 1000;
    static int javaStackSize = 1000;
    static RelativeTime t = new RelativeTime(5, 0);

    public TestInitializeSafelet() {
        super(null);
    }

    @Override
    @SCJAllowed(SUPPORT)
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
       return null;
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    @RunsIn("CDMission")
    protected void initialize() {
        MIRun miRun = new MIRun();
        @DefineScope(name="CDMission", parent=IMMORTAL)
        @Scope(IMMORTAL)
        ManagedMemory m = ManagedMemory.getCurrentManagedMemory();
        m.enterPrivateMemory(2000, miRun);

        //NOTE: no handler created
    }

    /**
     * A method to query the maximum amount of memory needed by this mission.
     *
     * @return the amount of memory needed
     */
    @Override
    public long missionMemorySize() {
        return 5420;
    }

    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    public void setUp() {
    }

    @SCJRestricted(CLEANUP)
    @SCJAllowed(SUPPORT)
    public void tearDown() {
    }

    @Override
    @SCJRestricted(CLEANUP)
    @SCJAllowed(SUPPORT)
    public void cleanUp() {
    }
}

@SCJAllowed(members=true)
@Scope("CDMission")
@DefineScope(name="CDMissionInit", parent="CDMission")
class MIRun implements Runnable {
    @RunsIn("CDMissionInit")
    public void run() {
        // ...
    }
}


