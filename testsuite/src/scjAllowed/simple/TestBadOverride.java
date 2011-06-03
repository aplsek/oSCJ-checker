///Users/plsek/_work/workspace_RT/scj-annotations/tests/scjAllowed/TestOverride401.java:29: Method may not decrease visibility of their overrides.
//    public int toOverride() {
//               ^
//1 error

package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.HIDDEN;
import static javax.safetycritical.annotate.Level.LEVEL_0;
import static javax.safetycritical.annotate.Level.LEVEL_2;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.ClockCallBack;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * @author plsek
 *
 */
@SCJAllowed(value=LEVEL_0)
public abstract class TestBadOverride extends Clock implements Runnable {
    @Override
    @SCJAllowed(LEVEL_2)
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_OVERRIDE
    protected void registerCallBack(AbsoluteTime time, ClockCallBack clockEvent) { }

    @Override
    @SCJAllowed(HIDDEN)
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_OVERRIDE
    public void run() { }
}
