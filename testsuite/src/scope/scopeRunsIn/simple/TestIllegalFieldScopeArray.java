package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
@Scope("a")
public abstract class TestIllegalFieldScopeArray extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestIllegalFieldScopeArray() {super(null, null);}

    X[] x;
    @Scope("a")
    static class X { }
    @Scope(IMMORTAL)
    static class Y {
        //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_FIELD_SCOPE
        X[] x;
    }
}
