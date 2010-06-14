//scjRestricted/TestPhase1.java:10: Illegal invocation of a method annotated CLEANUP, EXECUTION, or INITIALIZATION from within a method annotated ANY_TIME
//        bar();
//           ^
//scjRestricted/TestPhase1.java:11: Illegal invocation of a method annotated CLEANUP, EXECUTION, or INITIALIZATION from within a method annotated ANY_TIME
//        baz();
//           ^
//scjRestricted/TestPhase1.java:12: Illegal invocation of a method annotated CLEANUP, EXECUTION, or INITIALIZATION from within a method annotated ANY_TIME
//        quux();
//            ^
//scjRestricted/TestPhase1.java:18: Illegal invocation of a method annotated EXECUTION or INITIALIZATION from within a method annotated CLEANUP
//        baz();
//           ^
//scjRestricted/TestPhase1.java:19: Illegal invocation of a method annotated EXECUTION or INITIALIZATION from within a method annotated CLEANUP
//        quux();
//            ^
//scjRestricted/TestPhase1.java:24: Illegal invocation of a method annotated CLEANUP or EXECUTION from within a method annotated INITIALIZATION
//        bar();
//           ^
//scjRestricted/TestPhase1.java:26: Illegal invocation of a method annotated CLEANUP or EXECUTION from within a method annotated INITIALIZATION
//        quux();
//            ^
//scjRestricted/TestPhase1.java:31: Illegal invocation of a method annotated CLEANUP or INITIALIZATION from within a method annotated EXECUTION
//        bar();
//           ^
//scjRestricted/TestPhase1.java:32: Illegal invocation of a method annotated CLEANUP or INITIALIZATION from within a method annotated EXECUTION
//        baz();
//           ^
//9 errors

package scjRestricted;

import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;

public class TestPhase {
    @SCJRestricted(Restrict.ANY_TIME)
    public void foo() {
        foo();
        bar();
        baz();
        quux();
    }
    @SCJRestricted(Restrict.CLEANUP)
    public void bar() {
        foo();
        bar();
        baz();
        quux();
    }
    @SCJRestricted(Restrict.INITIALIZATION)
    public void baz() {
        foo();
        bar();
        baz();
        quux();
    }
    @SCJRestricted(Restrict.EXECUTION)
    public void quux() {
        foo();
        bar();
        baz();
        quux();
    }
}