//testsuite/src/scope/TestUpcast2.java:36: Static fields types must be @Scope(IMMORTAL) or nothing at all.
//   public static Lower el = new Lower();
//                       ^
//testsuite/src/scope/TestUpcast2.java:36: Object allocation in a context (IMMORTAL) other than its designated scope (a).
//   public static Lower el = new Lower();
//                            ^
//testsuite/src/scope/TestUpcast2.java:43: Class Cast Error : The class being casted must have a scope (@Scope=a) that is the same as the scope of the target class (@Scope=b).
//      TestUpcast2 f = (TestUpcast2) Lower.el; // should fail
//                      ^
//testsuite/src/scope/TestUpcast2.java:44: Cannot assign expression in scope a to variable in scope b.
//      TestUpcast2 g = Lower.el; // should fail
//                  ^
//4 errors

package scope;


import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;


public class TestUpcast2 {
    PrivateMemory a = new PrivateMemory(0);
    PrivateMemory b = new PrivateMemory(0);
}

@Scope("a")
@DefineScope(name = "a", parent = IMMORTAL)
class Lower extends TestUpcast2 {
   public static Lower el = new Lower();
}

@Scope ("b")
@DefineScope(name = "b", parent = "a")
class Middle {
  public void foo() {
      TestUpcast2 f = (TestUpcast2) Lower.el; // should fail
      TestUpcast2 g = Lower.el; // should fail
  }
}

