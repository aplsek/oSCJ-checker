///Users/plsek/_work/workspace_RT/scj-annotations/tests/scjAllowed/TestOverride401.java:29: Method may not decrease visibility of their overrides.
//    public int toOverride() {
//               ^
//1 error

package scjAllowed;

import javax.safetycritical.FakeSafelet;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;



/**
 * The first error is because the default constructor is level 1 and we call super(); which is level 0.
 * 
 * @author plsek
 *
 */
@SCJAllowed(Level.LEVEL_1)
public class TestOverride401 extends FakeSafelet {
    
    @SCJAllowed(Level.LEVEL_1)
    @Override
    public int toOverride() {
        return 0;
    }
}
