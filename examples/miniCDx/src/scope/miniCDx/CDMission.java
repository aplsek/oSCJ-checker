package scope.miniCDx;

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
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope("CDMission")
@SCJAllowed(members=true)
@DefineScope(name="CDMission", parent=IMMORTAL)
public class CDMission extends CyclicExecutive {

    //static PriorityParameters p = new PriorityParameters(18);
    // static StorageParameters s = new StorageParameters(1000L, 1000L, 1000L);

    static int priorityParameter = 18;
    static long totalBackingStore = 1000L;
    static long nativeStackSize = 1000L;
    static long javaStackSize = 1000L;
    static RelativeTime t = new RelativeTime(5, 0);

    public CDMission() {
        super(new PriorityParameters(priorityParameter),
              new StorageParameters(totalBackingStore, nativeStackSize, javaStackSize));
    }

    @Override
    @SCJAllowed(SUPPORT)
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
       return null;
      // TODO: issue 22:
      //  return new CyclicSchedule(
      //          new CyclicSchedule.Frame[] { new CyclicSchedule.Frame(t,
      //                  handlers) });
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    protected void initialize() {
        new CDHandler();
        MIRun miRun = new MIRun();
        @DefineScope(name="CDMission", parent=IMMORTAL)
        @Scope(IMMORTAL)
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
    @SCJAllowed(SUPPORT)
    public void run() {
        // ...
    }
}
