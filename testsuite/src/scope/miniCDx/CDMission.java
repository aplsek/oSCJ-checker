package scope.miniCDx;

import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;

@Scope("CDMission")
@SCJAllowed(members=true)
@DefineScope(name="CDMission", parent=Scope.IMMORTAL)
public class CDMission extends CyclicExecutive {

    static PriorityParameters p = new PriorityParameters(18);
    static StorageParameters s = new StorageParameters(1000L, 1000L, 1000L);
    static RelativeTime t = new RelativeTime(5, 0);

    @Override
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return new CyclicSchedule(
                new CyclicSchedule.Frame[] { new CyclicSchedule.Frame(t,
                        handlers) });
    }

    public CDMission() {
        super(p, s);
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    protected void initialize() {
        new CDHandler();
        MIRun miRun = new MIRun();
        @DefineScope(name="CDMissionInit", parent="CDMission")
        @Scope(Scope.IMMORTAL)
        ManagedMemory m = (ManagedMemory) ManagedMemory.getMemoryArea(this);
        m.enterPrivateMemory(2000, miRun);
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
    public void setUp() {
    }

    @SCJRestricted(CLEANUP)
    public void tearDown() {
    }

    @Override
    @SCJRestricted(CLEANUP)
    public void cleanUp() {
    }
}

@SCJAllowed(members=true)
@Scope("CDMission")
@DefineScope(name="CDMissionInit", parent="CDMission")
class MIRun implements SCJRunnable {
    @RunsIn("CDMissionInit")
    public void run() {
        // ...
    }
}
