package all.sanity;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.Clock;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
@Scope(IMMORTAL)
@DefineScope(name="MyApp", parent=IMMORTAL)
public class StaticAllocExample extends CyclicExecutive {

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("MyApp")
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        return new CyclicSchedule(
                new CyclicSchedule.Frame[] { new CyclicSchedule.Frame(new RelativeTime(5, 0),
                        handlers) });
    }

    @SCJRestricted(INITIALIZATION)
    public StaticAllocExample() {
        super(null);
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    @RunsIn("MyApp")
    public void initialize() {
        new MyPEH();
    }

    /**
     * A method to query the maximum amount of memory needed by this mission.
     *
     * @return the amount of memory needed
     */
    @Override
    @SCJAllowed(SUPPORT)
    public long missionMemorySize() {
        return 500; // MIN without printing is 500 bytes.
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

    @SCJAllowed(members=true)
    @Scope("MyApp")
    @DefineScope(name="MyPEH", parent="MyApp")
    public static class MyPEH extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public MyPEH() {
            super(new PriorityParameters(13),
                    new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                    500, 0)),
                    new StorageParameters(50L, null, 1000, 1000));
        }

        static int pos;
        static long[] times = new long[1000];

        @Override
        @RunsIn("MyPEH")
        @SCJAllowed(SUPPORT)
        public void handleAsyncEvent() {
            times[pos++] = Clock.getRealtimeClock().getTime().getMilliseconds();
            if (pos == 1000)
                Mission.getCurrentMission().requestSequenceTermination();
        }

        @Override
        @SCJRestricted(CLEANUP)
        @SCJAllowed(SUPPORT)
        public void cleanUp() {
        }

        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }
}
