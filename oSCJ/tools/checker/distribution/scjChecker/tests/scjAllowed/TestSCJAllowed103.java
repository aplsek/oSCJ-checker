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
    tests/scjAllowed/TestSCJAllowed103.java:29: warning: Illegal field access of an SCJ field.
        int variable = FakeSCJ.variable;
                              ^
    1 error
 * 
 * 
 * @author plsek
 */
@SCJAllowed(members = true, value = Level.LEVEL_0)
public class TestSCJAllowed103 {

    @SCJAllowed(Level.LEVEL_0)
    public void foo() {
        int variable = FakeSCJ.variable;
    }
}
