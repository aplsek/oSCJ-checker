package scope.scope.simple;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope(IMMORTAL)
@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestConstructor extends Mission {

    ///  @RunsIn annotations not allowed on constructors.
    @RunsIn("a")
    //## ERR_RUNSIN_ON_CONSTRUCTOR : TODO:
    public TestConstructor() {
    }
}
