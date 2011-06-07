package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.SCJAllowed;


// This is the same test as TestBadAssignmentScopeField except with upcasts
@SCJAllowed(members=true)
public class TestBadAssignmentScopeFieldUpcast {

    @Scope(IMMORTAL)
    @DefineScope(name="a", parent=IMMORTAL)
    static abstract class MS extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public MS() {super(null, null);}
    }

    static class X {
        Object y1;
        @Scope(IMMORTAL) Object y2;
        static Object y3;
    }

    static class Y { }

    void foo(X x, Y y, @Scope(IMMORTAL) Y yImm) {
        x.y1 = y;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y2 = y;
        x.y2 = yImm;
    }

    void bar(@Scope(IMMORTAL) X x, Y y, @Scope(IMMORTAL) Y yImm) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y1 = y;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        x.y2 = y;
        x.y1 = yImm;
        x.y2 = yImm;
    }

    void baz(Y y, @Scope(IMMORTAL) Y yImm) {
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        X.y3 = y;
        X.y3 = yImm;
    }
}
