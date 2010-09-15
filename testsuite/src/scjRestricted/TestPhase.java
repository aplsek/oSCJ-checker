//scjRestricted/TestPhase1.java:10: Illegal invocation of a method annotated CLEANUP, RUN, or INITIALIZATION from within a method annotated ALL
//        bar();
//           ^
//scjRestricted/TestPhase1.java:11: Illegal invocation of a method annotated CLEANUP, RUN, or INITIALIZATION from within a method annotated ALL
//        baz();
//           ^
//scjRestricted/TestPhase1.java:12: Illegal invocation of a method annotated CLEANUP, RUN, or INITIALIZATION from within a method annotated ALL
//        quux();
//            ^
//scjRestricted/TestPhase1.java:18: Illegal invocation of a method annotated RUN or INITIALIZATION from within a method annotated CLEANUP
//        baz();
//           ^
//scjRestricted/TestPhase1.java:19: Illegal invocation of a method annotated RUN or INITIALIZATION from within a method annotated CLEANUP
//        quux();
//            ^
//scjRestricted/TestPhase1.java:24: Illegal invocation of a method annotated CLEANUP or RUN from within a method annotated INITIALIZATION
//        bar();
//           ^
//scjRestricted/TestPhase1.java:26: Illegal invocation of a method annotated CLEANUP or RUN from within a method annotated INITIALIZATION
//        quux();
//            ^
//scjRestricted/TestPhase1.java:31: Illegal invocation of a method annotated CLEANUP or INITIALIZATION from within a method annotated RUN
//        bar();
//           ^
//scjRestricted/TestPhase1.java:32: Illegal invocation of a method annotated CLEANUP or INITIALIZATION from within a method annotated RUN
//        baz();
//           ^
//9 errors

package scjRestricted;

import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

public class TestPhase {
    @SCJRestricted(Phase.ALL)
    public void foo() {
        foo();
        bar();
        baz();
        quux();
    }
    @SCJRestricted(Phase.CLEANUP)
    public void bar() {
        foo();
        bar();
        baz();
        quux();
    }
    @SCJRestricted(Phase.INITIALIZATION)
    public void baz() {
        foo();
        bar();
        baz();
        quux();
    }
    @SCJRestricted(Phase.RUN)
    public void quux() {
        foo();
        bar();
        baz();
        quux();
    }
}