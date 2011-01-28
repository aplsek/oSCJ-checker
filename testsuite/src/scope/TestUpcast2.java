//scope/TestUpcast2.java:32: Object allocation in a context (IMMORTAL) other than its designated scope (a).
//   public static Lower el = new Lower();
//                            ^
//scope/TestUpcast2.java:38: Class Cast Error : The class being casted must have a scope (@Scope=IMMORTAL) that is the same as the scope of the target class (@Scope=b).
//      TestUpcast2 f = (TestUpcast2) Lower.el; // should fail
//                      ^
//scope/TestUpcast2.java:39: Cannot assign expression in scope IMMORTAL to variable in scope b.
//      TestUpcast2 g = Lower.el; // should fail
//                  ^
//3 errors


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

