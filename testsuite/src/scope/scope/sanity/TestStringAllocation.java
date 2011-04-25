package scope.scope.sanity;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope("a")
@DefineScope(name="a", parent=IMMORTAL)
@SCJAllowed(members=true)
public abstract class TestStringAllocation extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestStringAllocation() {super(null, null);}

    public String str = "string";

    public void method () {
        String str2 = "string";
    }
}
