//scope/TestOverride.java:16: error.override ???? 
//   ........           // 
//                ^
//1 error

package scope;


import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

public class TestOverride {
}

@DefineScope(name="ONE",parent=IMMORTAL)
class AClass {
      AClass a;
      public void X() { 
          a = new AClass();
      }
}

@Scope("ONE")
@DefineScope(name="TWO",parent="ONE")
class BClass extends AClass  {
    @RunsIn("TWO")
    public void method() {
      this.X();                     //  ERROR : because "X" is running in "ONE"
    }
}
