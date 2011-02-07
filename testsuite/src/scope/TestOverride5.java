//scope/TestOverride.java:16: error.override ???? 
//   ........           // 
//                ^
//1 error

package scope;


import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

public class TestOverride5 {
}

@DefineScope(name="ONE",parent=IMMORTAL)
class AClass55 {
      AClass a;
      public void X() { 
          a = new AClass();
      }
      
      public void method() {}
}

@Scope("ONE")
@DefineScope(name="TWO",parent="ONE")
class BClass55 extends AClass55  {
   
    @RunsIn("TWO")                  // ERROR ??
    public void method() {
      this.X();                    
    }
}
