package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
public class TestBadAssignmentScopeFieldArray {
    static Object[] os = new Object[1];

    void foo(@Scope(IMMORTAL) Object oImm) {
        os[0] = oImm;
        //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
        os[0] = new Object();
    }

    @RunsIn(IMMORTAL)
    void bar(Object o) {
        os[0] = o;
        os[0] = new Object();
    }
}
