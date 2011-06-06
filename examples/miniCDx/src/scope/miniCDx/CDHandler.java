package scope.miniCDx;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import java.util.List;

import javax.realtime.MemoryArea;
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
import static javax.safetycritical.annotate.Scope.IMMORTAL;


@SCJAllowed(members=true)
@Scope("CDMission")
@DefineScope(name="CDHandler", parent="CDMission")
public class CDHandler extends PeriodicEventHandler {
    StateTable st;
    boolean stop = false;

    static int priorityParameter = 13;
    static long totalBackingStore = 1000L;
    static long nativeStackSize = 1000L;
    static long javaStackSize = 1000L;
    static long periodicParameter = 500;

    /*
    static PriorityParameters pri;
    static PeriodicParameters per;
    static StorageParameters stor;

    static {
        pri = new PriorityParameters(13);
        per = new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                500, 0));
        stor = new StorageParameters(1000L, 1000L, 1000L);
    }
    */

    @SCJRestricted(INITIALIZATION)
    public CDHandler() {
        super(new PriorityParameters(priorityParameter),
                new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                        periodicParameter, 0)),
                new StorageParameters(totalBackingStore, nativeStackSize, javaStackSize));
        st = new StateTable();
    }

    @Override
    @RunsIn("CDHandler")
    @SCJAllowed(SUPPORT)
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
        @Scope("UNKNOWN") Vector3d old_pos = st.get(cs);
        if (old_pos == null) { // add new aircraft
            @Scope("CDMission") Callsign callsign = makeCallsign(cs);
            putCallSign(callsign);
        } else
            old_pos.update(); // update aircraft
        // }
        return null;
    }

    private final CallsignRunnable r = new CallsignRunnable();
    private final PutCallsignRunnable putRun = new PutCallsignRunnable();

    @RunsIn("CDHandler")
    public void putCallSign(@Scope("CDMission") Callsign callsign) {
        putRun.callsign = callsign;

        // ERROR: no @DefineScope!!
        ((ManagedMemory) ManagedMemory.getMemoryArea(st)).executeInArea(putRun);
    }

    @RunsIn("CDHandler") @Scope("CDMission")
    public Callsign makeCallsign(Callsign callsign) {
        try {
            @Scope(IMMORTAL)
            @DefineScope(name="CDMission", parent=IMMORTAL)
            ManagedMemory mem = (ManagedMemory) MemoryArea.getMemoryArea(r);
            r.cs = (byte[]) mem.newArrayInArea(r, byte.class,
                    callsign.cs.length);
            for (int i = 0; i < callsign.length; i++)
                r.cs[i] = callsign.cs[i];
            ((ManagedMemory) ManagedMemory.getMemoryArea(st)).executeInArea(r);
            return r.result;
        } catch (IllegalAccessException e) {
            //TODO:
            // e.printStackTrace();  //ERROR : is not allowed by SCJ
        }
        return null;
    }

    @Override
    @SCJAllowed(SUPPORT)
    public void cleanUp() {
    }

    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }

    @Scope("CDMission")
    class CallsignRunnable implements Runnable {
        byte[] cs;
        Callsign result;

        @RunsIn("CDMission")
        @SCJAllowed(SUPPORT)
        public void run() {
            result = new Callsign(cs);
        }
    }

    @Scope("CDMission")
    class PutCallsignRunnable implements Runnable {
        Callsign callsign;

        @RunsIn("CDMission")
        @SCJAllowed(SUPPORT)
        public void run() {
            st.put(callsign);
        }
    }
}
