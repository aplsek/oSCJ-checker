package scope.scope.sanity;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;


@SCJAllowed(members=true)
@Scope("a")
@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestNullAssignement extends Mission {

    A a = null;

    static class A {}
}
