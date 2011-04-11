package scope.scope.sanity;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;


@SCJAllowed(members=true)
@Scope("a")
@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestNullAssignement extends MissionSequencer {

    A a = null;

    @SCJRestricted(INITIALIZATION)
    public TestNullAssignement() {super(null, null);}

    static class A {}
}
