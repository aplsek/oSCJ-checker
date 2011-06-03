///Users/plsek/_work/workspace_RT/scj-annotations/tests/scjAllowed/TestOverride401.java:29: Method may not decrease visibility of their overrides.
//    public int toOverride() {
//               ^
//1 error

package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.HIDDEN;
import static javax.safetycritical.annotate.Level.LEVEL_0;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;
import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.ClockCallBack;
import javax.realtime.RelativeTime;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * @author plsek
 *
 */
@SCJAllowed(value=LEVEL_0)
public class TestBadOverride3 {
    @SCJAllowed(value=LEVEL_0)
    public void method() {}

    @SCJAllowed(SUPPORT)
    public void method2() {}

}

@SCJAllowed(value=LEVEL_0)
class Over extends TestBadOverride3 {

    @Override
    @SCJAllowed(SUPPORT)
    public void method() {}

    @Override
    @SCJAllowed(LEVEL_0)
    public void method2() {}
}