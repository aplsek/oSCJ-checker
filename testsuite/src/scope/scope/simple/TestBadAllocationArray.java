package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@DefineScope(name="a", parent=IMMORTAL)
@SCJAllowed(members = true)
public abstract class TestBadAllocationArray extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadAllocationArray() {super(null, null);}

    @Scope("a")
    static class X { }
    @RunsIn("a")
    void foo() {
        X[] x = new X[0];
    }
    @RunsIn(IMMORTAL)
    void bar() {
        //## checkers.scope.ScopeChecker.ERR_BAD_ALLOCATION_ARRAY
        X[] x = new X[0];
    }
}
