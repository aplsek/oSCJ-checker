//scopeDef/ScopeDefTest.java:13: Cyclical scope names detected.
//    public int a;
//               ^
//1 error

package scopeDef;

import javax.safetycritical.annotate.ScopeDef;

public class ScopeDefTest {
    @ScopeDef(name="x", parent="immortal")
    public int x;
    @ScopeDef(name="y", parent="a")
    public int y;
    @ScopeDef(name="z", parent="y")
    public int z;
    @ScopeDef(name="a", parent="y")
    public int a;
    public void foo() {
        @ScopeDef(name="b", parent="immortal")
        int b;
        b = 1;
        b = 2;
    }
}
