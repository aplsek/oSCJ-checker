package all.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.Clock;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.PriorityScheduler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@SCJAllowed(value = LEVEL_1, members = true)
public class Level1Hello implements Safelet {

    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    public MissionSequencer getSequencer() {
        return new OneSequencer(new PriorityParameters(PriorityScheduler
                .instance().getNormPriority()), new StorageParameters(100000L,null,
                1000, 1000));
    }

    @SCJAllowed(SUPPORT)
    @SCJRestricted(INITIALIZATION)
    public void setUp() {
    }

    @SCJRestricted(CLEANUP)
    @SCJAllowed(SUPPORT)
    public void tearDown() {
    }

    @Scope(IMMORTAL)
    @SCJAllowed(value = LEVEL_1, members = true)
    @DefineScope(name = "OneMission", parent = IMMORTAL)
    static public class OneSequencer extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        OneSequencer(PriorityParameters p, StorageParameters s) {
            super(p, s);
        }

        @Override
        @RunsIn("OneMission")
        @Scope("OneMission")
        @SCJAllowed(SUPPORT)
        protected Mission getNextMission() {
            return new OneMission();
        }
    }

    @Scope("OneMission")
    @SCJAllowed(value = LEVEL_1, members = true)
    static public class OneMission extends Mission {

        @SCJRestricted(INITIALIZATION)
        @Override
        @SCJAllowed(SUPPORT)
        public void initialize() {
            PEH peh = new PEH(new PriorityParameters(PriorityScheduler.instance()
                    .getNormPriority()), new PeriodicParameters(
                    new RelativeTime(0, 0), new RelativeTime(500, 0)),
                    new StorageParameters(50000L,null, 1000, 1000));
            peh.register();
        }

        @Override
        @SCJAllowed(SUPPORT)
        public long missionMemorySize() {
            return 1000L;
        }
    }

    @Scope("OneMission")
    @DefineScope(name = "PEH", parent = "OneMission")
    @SCJAllowed(value = LEVEL_1, members = true)
    static public class PEH extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        PEH(PriorityParameters p, PeriodicParameters r, StorageParameters s) {
            super(new PriorityParameters(13), new PeriodicParameters(
                    new RelativeTime(0, 0), new RelativeTime(500, 0)),
                    new StorageParameters(1000L, null, 1000, 1000));
        }

        int pos = 0;
        long[] times = new long[1000];
        long median;

        @Override
        @RunsIn("PEH")
        @SCJAllowed(SUPPORT)
        public void handleAsyncEvent() {
            times[pos++] = Clock.getRealtimeClock().getTime().getMilliseconds();
            long[] mytimes = new long[1000];
            for (int i = 0; i < 1000; i++)
                mytimes[i] = times[i];
            Arrays.sort(mytimes);
            median = mytimes[500];
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

    @Override
    @SCJAllowed(SUPPORT)
    public long immortalMemorySize() {
        // TODO Auto-generated method stub
        return 0;
    }
    

    @SCJAllowed(value = LEVEL_1, members = true)
    public static class Arrays {
        public static void sort(Object o) {}
    }
}
