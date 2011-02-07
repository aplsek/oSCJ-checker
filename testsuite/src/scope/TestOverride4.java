//scope/TestOverride.java:16: error.override ???? 
//   ........           // 
//                ^
//1 error

package scope;


import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

public class TestOverride4 {
}

@DefineScope(name="ONE",parent=IMMORTAL)
class AClass44 {
      AClass a;
      public void X() { 
          a = new AClass();
      }
}

@Scope("ONE")
@DefineScope(name="TWO",parent="ONE")
class BClass44 extends AClass44 implements Runnable {
   
    @RunsIn("TWO")                  // ERROR ??
    public void run() {
      this.X();                     // ERROR ??
    }
}
