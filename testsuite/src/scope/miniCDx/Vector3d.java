package scope.miniCDx;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;

import static javax.safetycritical.annotate.Scope.CALLER;

@SCJAllowed(members = true)
public class Vector3d {
    @RunsIn(CALLER)
    public void update() {
    }
}
