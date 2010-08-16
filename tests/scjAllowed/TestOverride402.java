//scjAllowed/TestOverride402.java:16: Subclasses may not decrease visibility of their superclasses.
//public class TestOverride402  extends ManagedEventHandler {
//       ^
//scjAllowed/TestOverride402.java:20: Method call is not allowed at level 0.
//        super();
//             ^
//2 errors

package scjAllowed;

import javax.safetycritical.FakeManagedEventHandler;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(Level.LEVEL_0)
public class TestOverride402  extends FakeManagedEventHandler {
   
    @SCJAllowed(Level.LEVEL_0)
    public TestOverride402 () {
        super();
    }
    
    @SCJAllowed(Level.LEVEL_0)
    public void foo() {
    }

}
