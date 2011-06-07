package scope.scope.sanity;

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
@SCJAllowed(members=true)
public class TestArray {
    @Scope("a")
    @SCJAllowed(members=true)
    static class ArrayObject { }

    @RunsIn("a")
    void foo(ArrayObject o) {
        ArrayObject[] a = new ArrayObject[1];
        a[0] = new ArrayObject();
        Object[] b = new Object[1];
        b[0] = new ArrayObject();
        a[0] = o;
        o = new ArrayObject(); // parameter assignability testing
        ArrayObject o2 = o;
    }

    @RunsIn("b")
    void bar(ArrayObject o) {
        ArrayObject[] a = new ArrayObject[1];
        a[0] = o;
    }

    @DefineScope(name="a", parent=IMMORTAL)
    @Scope(IMMORTAL)
    @SCJAllowed(members=true)
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}
    }
    @DefineScope(name="b", parent="a")
    @Scope("a")
    @SCJAllowed(members=true)
    static abstract class Y extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public Y() {super(null, null);} }
}
