package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.SUPPORT;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members=true)
public class TestBadSupport implements Runnable {
    @SCJAllowed(SUPPORT)
    public void foo() { }

    @Override
    public void run() { }
}
