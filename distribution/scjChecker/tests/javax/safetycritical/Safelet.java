package javax.safetycritical;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members = true, value = Level.LEVEL_0)
public class Safelet  {
    
    public Safelet() {
    }

    public static int getDeploymentLevel() {
        return 0;
    }

    public int toOverride() {
        return 0;
    }
}
