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
 *  tests/scjAllowed/SCJAllowedTest.java:38: warning: Illegal method call of an SCJ method.
        FakeSCJ.level1Call();
                          ^
    tests/scjAllowed/SCJAllowedTest.java:38: warning: Illegal field access of an SCJ field.
        FakeSCJ.level1Call();
               ^
 * 
 * @author plsek
 */
@SCJAllowed(members = true, value = Level.LEVEL_0)
public class SCJAllowedTest {
    
    @SCJAllowed(Level.LEVEL_0)
    public void foo() {
        FakeSCJ.level1Call();
        FakeSCJ.scjProtected();
        
        int variable = FakeSCJ.variable;
    }
}
