// no error here

package md5scj;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.*;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;

@SCJAllowed(members = true)
@Scope(IMMORTAL)
@DefineScope(name = "Level0App", parent = IMMORTAL)
public class Level0App extends CyclicExecutive {

    @SCJRestricted(INITIALIZATION)
    public Level0App() {
        super(null);
    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("Level0App")
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        CyclicSchedule.Frame[] frames = new CyclicSchedule.Frame[1];
        CyclicSchedule schedule = new CyclicSchedule(frames);
        frames[0] = new CyclicSchedule.Frame(new RelativeTime(Constants.PERIOD,
                0), handlers);
        return schedule;
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    @RunsIn("Level0App")
    public void initialize() {
        new MD5SCJ(Constants.PRIVATE_MEMORY, Constants.RUNS);
    }

    /**
     * A method to query the maximum amount of memory needed by this mission.
     *
     * @return the amount of memory needed
     */
    @Override
    public long missionMemorySize() {
        return Constants.MISSION_MEMORY;
    }

    @SCJAllowed(SUPPORT)
    @SCJRestricted(INITIALIZATION)
    public void setUp() {
    }

    @SCJAllowed(SUPPORT)
    @SCJRestricted(CLEANUP)
    public void tearDown() {
    }

    @Override
    @SCJAllowed(SUPPORT)
    @SCJRestricted(CLEANUP)
    public void cleanUp() {
    }
}
