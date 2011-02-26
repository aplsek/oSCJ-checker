package scope.override;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;


public class SimpleOverride {

}


class AClass {
      AClass a;
      public void methodX() {
      }

      public void methodY() {
          a = new AClass();
      }
}

@Scope("ONE")
@DefineScope(name="ONE",parent=IMMORTAL)
class BClass extends AClass  {

    public void method() {
      this.methodX();                     // OK
    }

    //public void method2() {
    //    this.methodY();                     // OK
    //}
}