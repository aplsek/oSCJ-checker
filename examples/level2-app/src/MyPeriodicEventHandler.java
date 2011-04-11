

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
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

@SCJAllowed(members=true, value=LEVEL_2)
@Scope("PrimaryMission")
@DefineScope(name="MyPeriodicEventHandler", parent="PrimaryMission")
public class MyPeriodicEventHandler extends PeriodicEventHandler {
    private static final int _priority = 17;
    private static final int _memSize = 5000;
    private int _eventCounter;

    @SCJRestricted(INITIALIZATION)
    public MyPeriodicEventHandler(String aehName, RelativeTime startTime,
            RelativeTime period) {
        super(new PriorityParameters(_priority), new PeriodicParameters(
                startTime, period), new StorageParameters(10000, 10000, 10000));
    }

    @Override
    @RunsIn("MyPeriodicEventHandler")
    public void handleAsyncEvent() {
        ++_eventCounter;
    }

    @SCJRestricted(CLEANUP)
    public void cleanup() {
    }
}
