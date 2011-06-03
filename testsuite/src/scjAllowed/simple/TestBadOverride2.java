///Users/plsek/_work/workspace_RT/scj-annotations/tests/scjAllowed/TestOverride401.java:29: Method may not decrease visibility of their overrides.
//    public int toOverride() {
//               ^
//1 error

package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.HIDDEN;
import static javax.safetycritical.annotate.Level.LEVEL_0;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

/**
 * The first error is because the default constructor is level 1 and we call super(); which is level 0.
 *
 * @author plsek
 *
 */
@SCJAllowed(members=true)
@Scope("PapaBenchL0")
@DefineScope(name="PapaBenchL0", parent=IMMORTAL)
public abstract class TestBadOverride2 extends CyclicExecutive {

    public TestBadOverride2(PriorityParameters priority,
            StorageParameters storage) {
        super(priority, storage);
    }

    @Override
    @SCJRestricted(INITIALIZATION)
    @RunsIn("PapaBenchL0")
    //## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_OVERRIDE_SUPPORT
    protected void initialize() {

    }

}