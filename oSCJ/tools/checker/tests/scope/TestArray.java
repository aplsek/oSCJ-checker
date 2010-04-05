//scope/TestArray.java:19: Variables of type @javax.safetycritical.annotate.Scope("a") ArrayObject are not allowed in this allocation context (immortal).
//    void foo(ArrayObject o) {
//                         ^
//scope/TestArray.java:20: Variables of type scope.ArrayObject  [] are not allowed in this allocation context (immortal).
//        ArrayObject[] a = new ArrayObject[1];
//                      ^
//scope/TestArray.java:21: Object allocation in a context (immortal) other than its designated scope (a).
//        a[0] = new ArrayObject();
//               ^
//scope/TestArray.java:39: Object allocation in a context (b) other than its designated scope (a).
//        a[0] = new ArrayObject();
//               ^
//4 errors

package scope;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("a")
class ArrayObject {
    
}

@Scope("immortal")
public class TestArray {
    @ScopeDef(name="a", parent="immortal")
    int a;
    @ScopeDef(name="b", parent="a")
    int b;
    
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
}
