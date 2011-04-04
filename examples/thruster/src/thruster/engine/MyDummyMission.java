package thruster.engine;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

/**
 * This class defines the mission that should not be executed.
 *
 * @author Lilei Zhai
 *
 */
@SCJAllowed(value = LEVEL_1, members=true)
@Scope("ThrusterMission")
public class MyDummyMission extends Mission {

    @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    protected void initialize() {
        // This Mission shall not be executed,
        //System.out
        //        .println("TestCase 23: FAIL. This Mission shall not be executed.");
    }

    @Override
    @SCJAllowed
    public long missionMemorySize() {
        //System.out
        //        .println("TestCase 23: FAIL. This Mission shall not be executed.");
        return 0;
    }

    @Override
    @SCJAllowed(SUPPORT)
    protected void cleanUp() {
        //System.out
        //        .println("TestCase 23: FAIL. This Mission shall not be executed.");
    }

}
