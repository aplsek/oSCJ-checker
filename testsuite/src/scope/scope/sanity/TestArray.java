package scope.scope.sanity;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope(IMMORTAL)
public class TestArray {
    @Scope("a")
    static class ArrayObject { }

    void foo(ArrayObject o) {
        ArrayObject[] a = new ArrayObject[1];
        a[0] = new ArrayObject();
        a[0] = o;
    }

    @RunsIn("a")
    void bar(ArrayObject o) {
        ArrayObject[] a = new ArrayObject[1];
        a[0] = new ArrayObject();
        Object[] b = new Object[1];
        b[0] = new ArrayObject();
        a[0] = o;
        o = new ArrayObject(); // parameter assignability testing
        ArrayObject o2 = o;
    }

    @RunsIn("b")
    void baz(ArrayObject o) {
        ArrayObject[] a = new ArrayObject[1];
        a[0] = new ArrayObject();
        a[0] = o;
    }

    static class Helper {
        static void foo() {
            ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(0,
                    new R1());
        }
        @Scope(IMMORTAL)
        @DefineScope(name="a", parent=IMMORTAL)
        static class R1 implements Runnable {
            @Override
            @RunsIn("a")
            public void run() {
                ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(0,
                        new R2());
            }
        }
        @Scope("a")
        @DefineScope(name="b", parent="a")
        static class R2 implements Runnable {

            @RunsIn("b") @Override
            public void run() { }
        }
    }
}
