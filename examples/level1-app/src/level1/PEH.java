package level1;


import java.util.Arrays;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.realtime.Clock;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;

@Scope("OneMission")
@DefineScope(name="PEH", parent="OneMission")
@SCJAllowed(value=LEVEL_1, members=true)
public class PEH extends PeriodicEventHandler {

    @SCJRestricted(INITIALIZATION)
    PEH(PriorityParameters p, PeriodicParameters r, StorageParameters s) {
        super(new PriorityParameters(13),
                new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                        500, 0)),
                        new StorageParameters(1000L, 1000L, 1000L));
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
