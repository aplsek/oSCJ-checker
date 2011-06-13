package sorter;
// no error here



import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import sorter.bench.BenchConf;

@SCJAllowed(members=true)
@Scope("Level0App")
@DefineScope(name="Level0App", parent=IMMORTAL)
public class SorterApp extends CyclicExecutive {

    @SCJRestricted(INITIALIZATION)
    public SorterApp() {
        super(null);
    }

    @Override
    @SCJAllowed(SUPPORT)
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        CyclicSchedule.Frame[] frames = new CyclicSchedule.Frame[1];
        frames[0] = new CyclicSchedule.Frame(new RelativeTime(BenchConf.PERIOD, 0), handlers);
        CyclicSchedule schedule = new CyclicSchedule(frames);
        return schedule;
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    public void initialize() {
        new SorterHandler(BenchConf.HANDLER_SCOPE_SIZE);
    }

    @Override
    public long missionMemorySize() {
        return BenchConf.MISSION_SCOPE_SIZE;
    }

    @Override
    @SCJAllowed(SUPPORT)
    @SCJRestricted(INITIALIZATION)
    public void setUp() {
    }

    @Override
    @SCJAllowed(SUPPORT)
    @SCJRestricted(CLEANUP)
    public void tearDown() {
        BenchConf.dump();
    }

}
