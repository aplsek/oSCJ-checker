package scjAllowed;

import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

public class TestBadSupport implements SCJRunnable {
    @SCJAllowed(Level.SUPPORT)
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_SUPPORT
    public void foo() { }

    @Override
    @SCJAllowed(Level.SUPPORT)
    public void run() { }
}
