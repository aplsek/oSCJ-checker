package scope.miniCDx;

import java.util.HashMap;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;
import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

@SCJAllowed(members = true)
@Scope("CDMission")
public class StateTable {
    Vector3d[] allocatedVectors = new Vector3d[1000];
    int usedVectors = 0;

    final HashMap motionVectors = new HashMap();
    final VectorRunnable r = new VectorRunnable();

    @RunsIn(UNKNOWN)
    public Vector3d get(Callsign cs) {
        return (Vector3d) motionVectors.get(cs);
    }

    @RunsIn(UNKNOWN)
    public void put(final Callsign cs) {

        if (ManagedMemory.allocInSame(r, cs))
            r.cs = cs; // DYNAMIC GUARD

        @Scope(Scope.IMMORTAL)
        @DefineScope(name = "CDMission", parent = Scope.IMMORTAL)
        MemoryArea m = MemoryArea.getMemoryArea(this);
        m.executeInArea(r);
    }

    @Scope("CDMission")
    class VectorRunnable implements Runnable {

        Callsign cs;

        public void run() {
            if (motionVectors.get(cs) != null)
                return;
            Vector3d v = allocatedVectors[usedVectors++];
            motionVectors.put(cs, v);
        }
    }

    @Scope("CDMission")
    class SCJVectorRunnable implements SCJRunnable {

        Callsign cs;

        @RunsIn("CDHandler")
        // TODO:??
        public void run() {
            if (motionVectors.get(cs) != null)
                return;
            Vector3d v = allocatedVectors[usedVectors++];
            motionVectors.put(cs, v);
        }
    }

}
