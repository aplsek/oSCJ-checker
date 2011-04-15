package staticAllocApp;


import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.realtime.Clock;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;

@SCJAllowed(members=true)
@Scope("MyApp")
@DefineScope(name="MyPEH", parent="MyApp")
public class MyPEH extends PeriodicEventHandler {

    @SCJRestricted(INITIALIZATION)
    public MyPEH() {
        super(new PriorityParameters(13),
                new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                500, 0)),
                new StorageParameters(50L, 1000L, 1000L));
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
