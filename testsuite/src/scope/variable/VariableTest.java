package scope.variable;


import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;




@DefineScope(name="Mission",parent=IMMORTAL)
@Scope("Mission")
public class VariableTest {
    
    public void method() {
        Foo foo; 
        foo = new Foo();
    }
}


class Foo {}