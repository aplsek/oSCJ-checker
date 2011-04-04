package scope.scope.sanity;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
public class TestArray {
    @Scope("a")
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
    static abstract class X extends Mission { }
    @DefineScope(name="b", parent="a")
    static abstract class Y extends Mission { }
}
