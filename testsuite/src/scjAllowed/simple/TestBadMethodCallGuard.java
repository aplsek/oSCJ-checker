package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.LEVEL_0;
import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.Services;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(value=LEVEL_0, members=true)
public class TestBadMethodCallGuard {
    public void foo() {

        if ( Services.getDeploymentLevel() == LEVEL_1) {
            bar();
        }
        //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_METHOD_CALL
        bar();
    }

    @SCJAllowed(LEVEL_1)
    public void bar() { }
}
