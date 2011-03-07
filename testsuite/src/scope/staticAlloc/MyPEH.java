package scope.staticAlloc;

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

@SCJAllowed(members = true)
@Scope("MyApp")
@DefineScope(name = "MyPEH", parent = "MyApp")
public class MyPEH extends PeriodicEventHandler {

    static PriorityParameters pri;
    static PeriodicParameters per;
    static StorageParameters stor;

    static {
        pri = new PriorityParameters(13);
        per = new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                500, 0));
        stor = new StorageParameters(50L, 1000L, 1000L);
    }

    @SCJRestricted(INITIALIZATION)
    public MyPEH() {
        super(pri, per, stor);
    }

    static int pos;
    static long[] times = new long[1000];

    @Override
    @RunsIn("MyPEH")
    public void handleAsyncEvent() {
        times[pos++] = Clock.getRealtimeClock().getTime().getMilliseconds();
        if (pos == 1000)
            Mission.getCurrentMission().requestSequenceTermination();
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    public void cleanUp() {
    }

    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}
