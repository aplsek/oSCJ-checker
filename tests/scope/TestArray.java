//scope/TestArray.java:38: Variables of type ArrayObject are not allowed in this allocation context (immortal).
//    void foo(ArrayObject o) {
//                         ^
//scope/TestArray.java:39: Variables of type scope.ArrayObject  [] are not allowed in this allocation context (immortal).
//        ArrayObject[] a = new ArrayObject[1];
//                      ^
//scope/TestArray.java:40: Cannot assign expression in scope immortal to variable in scope a.
//        a[0] = new ArrayObject();
//             ^
//scope/TestArray.java:40: Object allocation in a context (immortal) other than its designated scope (a).
//        a[0] = new ArrayObject();
//               ^
//scope/TestArray.java:57: Object allocation in a context (b) other than its designated scope (a).
//        ArrayObject[] a = new ArrayObject[1];
//                          ^
//scope/TestArray.java:58: Cannot assign expression in scope b to variable in scope a.
//        a[0] = new ArrayObject();
//             ^
//scope/TestArray.java:58: Object allocation in a context (b) other than its designated scope (a).
//        a[0] = new ArrayObject();
//               ^
//7 errors

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope("a")
class ArrayObject {
    
}

@Scope("immortal")
public class TestArray {
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
            ManagedMemory.
                getCurrentManagedMemory().
                    enterPrivateMemory(0, new /*@DefineScope(name="a", parent="immortal")*/ R1());
        }
        @Scope("immortal") @RunsIn("a")
        static class R1 implements Runnable {
            @Override
            public void run() {
                ManagedMemory.
                    getCurrentManagedMemory().
                        enterPrivateMemory(0, new /*@DefineScope(name="b", parent="a")*/ R2());
            }
        }
        @Scope("a") @RunsIn("b")
        static class R2 implements Runnable {
            @Override
            public void run() {
            }
        }
    }
}
