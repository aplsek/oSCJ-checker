package scjAllowed;

import javax.safetycritical.FakeSCJ;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
/**
 * ERROR:
 * tests/scjAllowed/TestSCJAllowed101.java:11: warning: Illegal method call of an SCJ method.
        FakeSCJ.level1Call();
                          ^
    tests/scjAllowed/TestSCJAllowed101.java:11: warning: Illegal field access of an SCJ field.
        FakeSCJ.level1Call();
               ^
    2 errors
 * 
 * @author plsek
 *
 */
@SCJAllowed(members = true, value = Level.LEVEL_0)
public class TestSCJAllowed101 {
    @SCJAllowed(Level.LEVEL_0)
    public void foo() {
        FakeSCJ.level1Call();
    }
}
