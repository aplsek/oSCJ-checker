package scope.miniCDx;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import java.util.HashMap;

import javax.realtime.MemoryArea;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.CALLER;

@SCJAllowed(members=true)
@Scope("CDMission")
public class StateTable {
    Vector3d[] allocatedVectors = new Vector3d[1000];
    int usedVectors = 0;

    final HashMap motionVectors = new HashMap();
    final VectorRunnable r = new VectorRunnable();

    @RunsIn(CALLER)
    public Vector3d get(@Scope(UNKNOWN) Callsign cs) {   // TODO: should this be UNKNOWN??
        return (Vector3d) motionVectors.get(cs);
    }

    @RunsIn(CALLER)
    public void put(@Scope(UNKNOWN) final Callsign cs) {

        if (ManagedMemory.allocInSame(r, cs))
            r.cs = cs; // DYNAMIC GUARD

        @Scope(IMMORTAL)
        @DefineScope(name="CDMission", parent=IMMORTAL)
        MemoryArea m = MemoryArea.getMemoryArea(this);
        m.executeInArea(r);
    }

    @Scope("CDMission")
    class VectorRunnable implements SCJRunnable {

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
