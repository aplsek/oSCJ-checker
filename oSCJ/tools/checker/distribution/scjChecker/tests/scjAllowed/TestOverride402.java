package scjAllowed;

import javax.safetycritical.ManagedEventHandler;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * ERRORS:
 * ./tests/scjAllowed/OverrideConstructorTest.java:21: Illegal method call of an SCJ class super() constructor.
    public OverrideConstructorTest() {
                                     ^
    1 error 
 * 
 * @author plsek
 *
 */
@SCJAllowed(Level.LEVEL_0)
public class TestOverride402  extends ManagedEventHandler {
   
    @SCJAllowed(Level.LEVEL_0)
    public TestOverride402() {
    }
}
