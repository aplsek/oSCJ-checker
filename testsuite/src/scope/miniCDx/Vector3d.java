package scope.miniCDx;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;

import static javax.safetycritical.annotate.Scope.UNKNOWN;

@SCJAllowed(members = true)
public class Vector3d {
    @RunsIn(UNKNOWN)
    public void update() {
    }
}
