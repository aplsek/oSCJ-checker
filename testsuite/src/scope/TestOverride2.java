//scope/TestOverride2.java:16: error.override ???? 
//   ........           // 
//                ^
//1 error

package scope;


import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

public class TestOverride2 {
}

@Scope("ONE")
@DefineScope(name="ONE",parent=IMMORTAL)
class AClass2 {
      AClass2 a;
      public void X() { 
          a = new AClass2();
      }
}

@Scope("TWO")
@DefineScope(name="TWO",parent="ONE")
class BClass2 extends AClass2 implements Runnable {
    @RunsIn("TWO")
    public void run() {
      this.X();
    }
}
