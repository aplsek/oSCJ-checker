package perReleaseAllocApp;


import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import java.util.Arrays;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
@Scope("MyApp")
@DefineScope(name="MyPEH", parent="MyApp")
public class MyPEH extends PeriodicEventHandler {

    static int priority  = 13;
    static long relativeTime = 500;

    @SCJRestricted(INITIALIZATION)
    public MyPEH() {
        super(new PriorityParameters(priority),
                new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                        500, 0)),
                new StorageParameters(1000L, 1000L, 1000L));
    }

    long median;

    @Override
    @RunsIn("MyPEH")
    @SCJAllowed(SUPPORT)
    public void handleAsyncEvent() {
        final long times[] = new long[1000];

        MySCJRunnable r = new MySCJRunnable();
        r.times = times;

        @Scope("MyApp")
        @DefineScope(name="MyPEH", parent="MyApp")
        ManagedMemory m = ManagedMemory.getCurrentManagedMemory();
        m.enterPrivateMemory(8000, r);
        median = r.median;

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

    @SCJAllowed(members=true)
    @DefineScope(name="RunScope", parent ="MyPEH")
    static class MySCJRunnable implements Runnable {
        public long times[];
        long median;

        @RunsIn("RunScope")
        @SCJAllowed(SUPPORT)
        public void run() {
            long[] copy = new long[1000];
            for (int i = 0; i < 1000; i++)
                copy[i] = times[i];
            Arrays.sort(copy);
            median = copy[500];
        }
    };

}
