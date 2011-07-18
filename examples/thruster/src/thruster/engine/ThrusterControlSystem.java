package thruster.engine;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;

import javax.safetycritical.MissionSequencer;
import javax.safetycritical.Safelet;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;


/**
 * This is the application!
 *
 * @author plsek
 *
 */
@SCJAllowed(value = LEVEL_1, members = true)
@Scope(IMMORTAL)
public class ThrusterControlSystem implements Safelet {

    @SCJRestricted(INITIALIZATION)
    public ThrusterControlSystem() {
        // System.out.println("Safelet being created");
    }

    /**
     * This method returns the MissionSequencer of the application.
     * "myMissionSequencer" takes a reference of the MissionSequencer. If
     * "myMissionSequencer" is null, create a new MissionSequencer, otherwise
     * return the "myMissionSequencer" directly.
     */
    @Override
    @SCJAllowed(SUPPORT)
    @SCJRestricted(INITIALIZATION)
    @Scope(IMMORTAL) @RunsIn(IMMORTAL)
    public MissionSequencer getSequencer() {
        // System.out.println("MissionSequencer.getSequencer() is executed.");
        return ThrusterControlSequencer.getInstance();
    }

    @Override
    public long immortalMemorySize() {
        // TODO Auto-generated method stub
        return 0;
    }

}
