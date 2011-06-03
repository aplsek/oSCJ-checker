package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
@Scope(IMMORTAL)
@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestRunsInOnConstructor extends MissionSequencer {

    @RunsIn("a")
    @SCJRestricted(INITIALIZATION)
    //## checkers.scope.ScopeRunsInChecker.ERR_RUNS_IN_ON_CONSTRUCTOR
    public TestRunsInOnConstructor() {super(null, null); }
}
