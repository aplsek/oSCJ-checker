package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.SUPPORT;

import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class TestBadSupport implements SCJRunnable {
    @SCJAllowed(SUPPORT)
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_SUPPORT
    public void foo() { }

    @Override
    @SCJAllowed(SUPPORT)
    public void run() { }
}
