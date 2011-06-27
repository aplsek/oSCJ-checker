package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.*;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
@Scope(IMMORTAL)
@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestIllegalFieldScope extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestIllegalFieldScope() {super(null, null);}


    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_FIELD_SCOPE
    @Scope("a") Object o;
    int[] o2;

    //## checkers.scope.ScopeRunsInChecker.ERR_BAD_SCOPE_NAME
    @Scope(CALLER) Object oo;

    @Scope(THIS) Object ooo;
}
