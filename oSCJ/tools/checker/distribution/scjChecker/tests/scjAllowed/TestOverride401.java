package scjAllowed;

import javax.safetycritical.Safelet;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * ERRORS:
 * tests/scjAllowed/OverrideTest.java:21: warning: Method must have the same SCJ visibility as all overridden methods.
    public int getDeploymentLevel() {
               ^
 * 
 * @author plsek
 *
 */
@SCJAllowed(Level.LEVEL_1)
public class TestOverride401 extends Safelet {
    @SCJAllowed(Level.LEVEL_1)
    @Override
    public int toOverride() {
        return 0;
    }
}
