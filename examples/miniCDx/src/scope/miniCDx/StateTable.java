package scope.miniCDx;

import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.CALLER;

@SCJAllowed(members=true)
@Scope("CDMission")
public class StateTable {
    Vector3d[] allocatedVectors = new Vector3d[1000];
    int usedVectors = 0;

    final MyHashMap motionVectors = new MyHashMap();

    @RunsIn(CALLER) @Scope("CDMission")
    public Vector3d get(@Scope(UNKNOWN) Callsign cs) {   // TODO: should this be UNKNOWN??
        return (Vector3d) motionVectors.get(cs);
    }

    Callsign css = null;

    public void put(@Scope(UNKNOWN) final Callsign cs) {
        if (ManagedMemory.allocInSame(this, cs))
            css = cs; // DYNAMIC GUARD

        if (motionVectors.get(css) != null)
            return;
        Vector3d v = allocatedVectors[usedVectors++];
        motionVectors.put(css, v);
    }
}
