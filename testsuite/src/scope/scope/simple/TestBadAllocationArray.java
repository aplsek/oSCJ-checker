package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

public class TestBadAllocationArray {
    @Scope(IMMORTAL)
    static class X { }
    void foo() {
        // Using Object on the LHS because a variable of type X[] is IMMORTAL
        //## checkers.scope.ScopeChecker.ERR_BAD_ALLOCATION
        Object x = new X[0];
    }
    @RunsIn(IMMORTAL)
    void bar() {
        X[] x = new X[0];
    }
    static X[] x = new X[0];
}
