package scjAllowed;

import javax.safetycritical.FakeSCJ;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;


/**
 * 
 * Testing :
 *       - SCJAllowed 
 *             - by calling an SCJ method with higher level
 *       - SCJProtected
 * 
 * ERRORS: 
 * 
 * 
    tests/scjAllowed/SCJAllowedTest.java:36: warning: Illegal method call of an @SCJProtected method.
        FakeSCJ.scjProtected();
                            ^
                             1 error
 * 
 * 
 * @author plsek
 */
@SCJAllowed(members = true, value = Level.LEVEL_0)
public class TestSCJAllowed102 {

    @SCJAllowed(Level.LEVEL_0)
    public void foo() {
        FakeSCJ.scjProtected();
    }
}
