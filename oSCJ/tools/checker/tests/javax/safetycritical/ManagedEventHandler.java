package javax.safetycritical;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members = true, value = Level.LEVEL_1)
public class ManagedEventHandler {
  
    @SCJAllowed(Level.LEVEL_1)
    public void foo() {}
}
