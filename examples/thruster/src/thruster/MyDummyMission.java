package thruster;

import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * This class defines the mission that should not be executed.
 *
 * @author Lilei Zhai
 *
 */
@SCJAllowed(value = LEVEL_1, members=true)
public class MyDummyMission extends Mission {

    @Override
    protected void initialize() {
        // This Mission shall not be executed,
        //System.out
        //        .println("TestCase 23: FAIL. This Mission shall not be executed.");
    }

    @Override
    public long missionMemorySize() {
        //System.out
        //        .println("TestCase 23: FAIL. This Mission shall not be executed.");
        return 0;
    }

    protected void cleanup() {
        //System.out
        //        .println("TestCase 23: FAIL. This Mission shall not be executed.");
    }

}
