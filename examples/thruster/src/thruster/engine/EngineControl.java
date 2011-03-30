package thruster.engine;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

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

@SCJAllowed(value = LEVEL_1, members=true)
@Scope("ThrusterMission")
@DefineScope(name="EngineControl", parent="ThrusterMission")
public class EngineControl extends PeriodicEventHandler {

    private boolean engineStarted = false;
    private RelativeTime burnTime;
    private RelativeTime zero;
    private PeriodicParameters myPeriodicParams;

    @SCJRestricted(INITIALIZATION)
    public EngineControl(PriorityParameters priority,
            PeriodicParameters release, StorageParameters storage, long memSize) {
        super(priority, release, storage);
        //System.out.println("Engine Control handler constructor ");
        myPeriodicParams = release;
        zero = new RelativeTime(0, 0);
    }

    public synchronized void start(RelativeTime duration) {
        if (engineStarted)
            return;
        engineStarted = true;
        burnTime = new RelativeTime(duration);
        //System.out.println("Engine start");
    }

    @RunsIn("EngineControl")
    public synchronized void stop() {
        engineStarted = false;
        //System.out.println("Engine stop");
    }

    // The following method executes in a fresh private memory area
    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("EngineControl")
    public synchronized void handleAsyncEvent() {
        if (engineStarted) {
            if (burnTime.compareTo(zero) == 0) {
                stop();
            } else {
                burnTime.subtract(myPeriodicParams.getPeriod());
                //System.out.println("Engine Control");
                // adjust valve to ensure no mechanical drift
                // and, thereby, get an steady fuel flow
            }
        }
    }
}
