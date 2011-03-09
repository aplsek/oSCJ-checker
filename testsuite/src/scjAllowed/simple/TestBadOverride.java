///Users/plsek/_work/workspace_RT/scj-annotations/tests/scjAllowed/TestOverride401.java:29: Method may not decrease visibility of their overrides.
//    public int toOverride() {
//               ^
//1 error

package scjAllowed.simple;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.ClockCallBack;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;



/**
 * The first error is because the default constructor is level 1 and we call super(); which is level 0.
 *
 * @author plsek
 *
 */
@SCJAllowed(value=Level.LEVEL_0)
public abstract class TestBadOverride extends Clock implements SCJRunnable {
    @Override
    @SCJAllowed(Level.LEVEL_2)
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_OVERRIDE
    protected void registerCallBack(AbsoluteTime time, ClockCallBack clockEvent) { }

    @Override
    @SCJAllowed(Level.HIDDEN)
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_OVERRIDE
    public void run() { }
}
