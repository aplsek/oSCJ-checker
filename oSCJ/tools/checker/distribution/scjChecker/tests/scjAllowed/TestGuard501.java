package scjAllowed;

import javax.safetycritical.FakeSCJ;
import javax.safetycritical.Safelet;
/**
 * 
 * ERRORS:
       tests/scjAllowed/GuardTest.java:25: warning: Illegal method call of an SCJ method.
            FakeSCJ.level1Call();
                              ^
        tests/scjAllowed/GuardTest.java:25: warning: Illegal field access of an SCJ field.
            FakeSCJ.level1Call();
                   ^
 * 
 * 
 * @author plsek
 *
 */
public class TestGuard501 {
    
    public void Foo() {

        if (Safelet.getDeploymentLevel() == 0) {
            // we are in the guard!
            FakeSCJ.level1Call();
        }

        FakeSCJ.level1Call();
    }
    
}
