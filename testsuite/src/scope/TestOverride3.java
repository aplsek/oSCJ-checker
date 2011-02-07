//scope/TestOverride3.java:16: error.override ???? 
//    public void run() {           
//                ^
//1 error

package scope;


import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

public class TestOverride3 {
}

@Scope("ONE")
@DefineScope(name="ONE",parent=IMMORTAL)
class AClass3 {
      AClass3 a;
      @RunsIn("ONE")
      public void X() { 
          a = new AClass3();
      }
}

@Scope("ONE")
@DefineScope(name="TWO",parent="ONE")
class BClass3 extends AClass3 implements Runnable {
    @RunsIn("TWO")                                          // ERROR?
    public void run() {         
      this.X();
    }
}
