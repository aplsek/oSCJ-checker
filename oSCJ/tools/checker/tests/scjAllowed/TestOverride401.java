//scjAllowed/TestOverride401.java:16: Method may not decrease visibility of their overrides.
//    public int toOverride() {
//               ^
//1 error

package scjAllowed;

import javax.safetycritical.Safelet;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(Level.LEVEL_1)
public class TestOverride401 extends Safelet {
    @SCJAllowed(Level.LEVEL_1)
    @Override
    public int toOverride() {
        return 0;
    }
}
