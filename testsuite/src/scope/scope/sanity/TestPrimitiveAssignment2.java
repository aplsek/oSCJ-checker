package scope.scope.sanity;

import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
@Scope("a")
public abstract class TestPrimitiveAssignment2 extends Mission {

    @RunsIn(CALLER)
    final void method(long long_result) {
      serviceRequest(long_result);
    }

    // invoked by SecurityService sub-mission
    @RunsIn(CALLER)
    synchronized final void serviceRequest(long result) {
    }
}