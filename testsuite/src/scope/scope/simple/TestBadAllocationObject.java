package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

public class TestBadAllocationObject {
    @Scope(IMMORTAL)
    static class X { }
    void foo() {
        //## checkers.scope.ScopeChecker.ERR_BAD_ALLOCATION
        new X();
    }
    @RunsIn(IMMORTAL)
    void bar() {
        new X();
    }
    static X x = new X();
}
