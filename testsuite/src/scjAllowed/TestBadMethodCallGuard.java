package scjAllowed;

import javax.safetycritical.Services;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(value=Level.LEVEL_0, members=true)
public class TestBadMethodCallGuard {
    public void foo() {

        if ( Services.getDeploymentLevel() == Level.LEVEL_1) {
            bar();
        }
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_METHOD_CALL
        bar();
    }

    @SCJAllowed(Level.LEVEL_1)
    public void bar() { }
}
