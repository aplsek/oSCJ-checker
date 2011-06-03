package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.SUPPORT;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members=true)
public class TestBadSupport implements Runnable {
    @SCJAllowed(SUPPORT)
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_SUPPORT
    public void foo() { }

    @Override
    public void run() { }
}
