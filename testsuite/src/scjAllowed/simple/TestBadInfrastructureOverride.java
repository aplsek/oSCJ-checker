package scjAllowed.simple;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(value=Level.LEVEL_1, members=true)
public abstract class TestBadInfrastructureOverride extends PrivateMemory {
    public TestBadInfrastructureOverride(long size) {
        super(size);
    }

    @Override
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_INFRASTRUCTURE_OVERRIDE
    public void enter(Runnable logic) { }
}

