package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;


// This is the same test as TestBadAssignmentScopeField except with upcasts
@SCJAllowed(members=true)
public class TestCast {

    @RunsIn(IMMORTAL)
    void bar(Foo foo) {
        Bar obj = (Bar) foo;
    }

    void method(Foo foo) {
      foo(foo);
    }

    void foo(Object obj) {
    }

    class Foo {}

    @Scope(IMMORTAL)
    class Bar extends Foo{}

    @DefineScope(name="a", parent=IMMORTAL)
    static abstract class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}

        Object y1;
        @Scope(IMMORTAL) Object y2;
        static Object y3;
    }
    static class Y { }

}
