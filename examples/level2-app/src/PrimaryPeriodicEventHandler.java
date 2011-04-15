

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

@SCJAllowed(members=true, value=LEVEL_2)
@Scope("PrimaryMission")
@DefineScope(name="PrimaryPeriodicEventHandler", parent="PrimaryMission")
public class PrimaryPeriodicEventHandler extends PeriodicEventHandler {
    private static final int _priority = 17;
    private static final int _memSize = 5000;
    private int _eventCounter;

    @SCJRestricted(INITIALIZATION)
    public PrimaryPeriodicEventHandler(String aehName, RelativeTime startTime,
            RelativeTime period) {
        super(new PriorityParameters(_priority), new PeriodicParameters(
                startTime, period), new StorageParameters(10000, 10000, 10000));
    }

    @Override
    @RunsIn("PrimaryPeriodicEventHandler")
    @SCJAllowed(SUPPORT)
    public void handleAsyncEvent() {
        ++_eventCounter;
    }

    @Override
    @SCJRestricted(CLEANUP)
    @SCJAllowed(SUPPORT)
    public void cleanUp() {
    }
}
