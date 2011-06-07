package scope.scope.sanity;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Level.SUPPORT;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@DefineScope(name="D", parent=IMMORTAL)
@SCJAllowed(members=true)
public class TestPrimitiveAssignment extends MissionSequencer {

    private int prior_longitude, prior_lattitude;
    private int longitude, lattitude;

    @SCJRestricted(INITIALIZATION)
    public TestPrimitiveAssignment() {super(null, null);}

    // called periodically by the GPS Driver
    @RunsIn(CALLER)
    synchronized void updatePosition(int longitude, int lattitude) {
      prior_longitude = this.longitude;
      prior_lattitude = this.lattitude;

      this.longitude = longitude;
      this.lattitude = lattitude;

    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("D")
    protected Mission getNextMission() {
        return null;
    }
}
