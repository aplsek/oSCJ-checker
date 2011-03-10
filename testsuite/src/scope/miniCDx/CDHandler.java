package scope.miniCDx;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import java.util.List;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
@Scope("CDMission")
@DefineScope(name="CDHandler", parent="CDMission")
public class CDHandler extends PeriodicEventHandler {
    StateTable st;
    boolean stop = false;

    static PriorityParameters pri;
    static PeriodicParameters per;
    static StorageParameters stor;

    static {
        pri = new PriorityParameters(13);
        per = new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                500, 0));
        stor = new StorageParameters(1000L, 1000L, 1000L);
    }

    @SCJRestricted(INITIALIZATION)
    public CDHandler() {
        super(pri, per, stor);
        st = new StateTable();
    }

    @Override
    @RunsIn("CDHandler")
    public void handleAsyncEvent() {
        // ...
        createMotions(null);
        if (stop)
            Mission.getCurrentMission().requestSequenceTermination();
    }

    @RunsIn("CDHandler")
    public List createMotions(Frame fr) {
        // for (Callsign cs : fr.getCallsigns()) {
        Callsign cs = null;
        @Scope("UNKNOWN")
        Vector3d old_pos = st.get(cs);
        if (old_pos == null) { // add new aircraft
            Callsign callsign = makeCallsign(cs);
            st.put(callsign);
        } else
            old_pos.update(); // update aircraft
        // }
        return null;
    }

    private final CallsignRunnable r = new CallsignRunnable();

    @RunsIn("CDHandler")
    public Callsign makeCallsign(Callsign callsign) {
        try {
            r.cs = (byte[]) ManagedMemory.newArrayInArea(r, byte.class,
                    callsign.cs.length);
            for (int i = 0; i < callsign.length; i++)
                r.cs[i] = callsign.cs[i];
            ManagedMemory.getMemoryArea(st).executeInArea(r);
            return r.result;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void cleanUp() {
    }

    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }

    @Scope("CDMission")
    class CallsignRunnable implements SCJRunnable {
        byte[] cs;
        Callsign result;

        //@RunsIn("CDMission")
        public void run() {
            result = new Callsign(cs);
        }
    }
}
