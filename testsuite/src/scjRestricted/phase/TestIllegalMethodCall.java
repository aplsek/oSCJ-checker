package scjRestricted.phase;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestIllegalMethodCall {
    @SCJRestricted(Phase.ALL)
    public void foo() {
        foo();
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_METHOD_CALL
        bar();
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_METHOD_CALL
        baz();
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_METHOD_CALL
        quux();
    }
    @SCJRestricted(Phase.CLEANUP)
    public void bar() {
        foo();
        bar();
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_METHOD_CALL
        baz();
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_METHOD_CALL
        quux();
    }
    @SCJRestricted(Phase.INITIALIZATION)
    public void baz() {
        foo();
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_METHOD_CALL
        bar();
        baz();
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_METHOD_CALL
        quux();
    }
    @SCJRestricted(Phase.RUN)
    public void quux() {
        foo();
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_METHOD_CALL
        bar();
        //## checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_METHOD_CALL
        baz();
        quux();
    }
}