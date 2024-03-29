package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;



@Scope(IMMORTAL)
@DefineScope(name="a", parent=IMMORTAL)
@SCJAllowed(members = true)
public abstract class TestBadAssignmentScopeLocalArray extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadAssignmentScopeLocalArray() {super(null, null);}


    @RunsIn("a")
    void foo(@Scope(IMMORTAL) Object[] os, final @Scope(UNKNOWN) Object[] os2,
            final @Scope(IMMORTAL) Object oImm, X x) {
        os[0] = oImm;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        os[0] = new Object();

        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        os2[0] = oImm;
        if (ManagedMemory.allocatedInSame(os2, oImm))
            os2[0] = oImm;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        os2[0] = x;
    }

    void bar(Object[] os, Object o) {
        os[0] = o;
        os[0] = new Object();
    }

    @Scope("a")
    static class X { }

    static class Y { }

    @RunsIn("a")
    void baz(@Scope(UNKNOWN) X[] xs, X x) {
        xs[0] = x;

        X[] xx;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        xx = xs;
    }

    @RunsIn("a")
    void baz(@Scope(UNKNOWN) Y[] ys, Y y) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        ys[0] = y;
    }
}
