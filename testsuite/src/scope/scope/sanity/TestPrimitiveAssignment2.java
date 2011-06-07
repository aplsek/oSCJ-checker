package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
@Scope(IMMORTAL)
@SCJAllowed(members = true)
public abstract class TestPrimitiveAssignment2 extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestPrimitiveAssignment2() {super(null, null);}

    @RunsIn(CALLER)
    final void method(long long_result) {
      serviceRequest(long_result);
    }

    // invoked by SecurityService sub-mission
    @RunsIn(CALLER)
    synchronized final void serviceRequest(long result) {
    }
}
