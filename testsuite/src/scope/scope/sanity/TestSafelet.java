package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.MissionSequencer;
import javax.safetycritical.Safelet;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;


/**
 * This class is the Safelet for unit test. This unit test program is also an example level one
 * SCJ application.
 */
@SCJAllowed(value = LEVEL_1, members=true)
@Scope(IMMORTAL)
public class TestSafelet implements Safelet {

    @SCJRestricted(INITIALIZATION)
	public TestSafelet() {
		//System.out.println("Safelet being created");
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
	    return null;
	}

	/**
	 * This method will be called before "getSequencer()". This method may
	 * create objects in ImmortalMemoryArea, set up hardware interrupt handlers,
	 * or initializing hardware devices.
	 */
    @SCJAllowed(SUPPORT)
    @SCJRestricted(INITIALIZATION)
	public void setUp() {
		/*
		 * This testing method doesn't contain any operation, just print
		 * something to the console showing it is called.
		 */
		//System.out.println("TestCase 01: PASS. MissionSequencer.setUp() is executed.");
	}

	/**
	 * This method will be called after the termination of the MissionSequencer.
	 * It frees all resources used by the application.
	 */
    @SCJAllowed(SUPPORT)
    @SCJRestricted(CLEANUP)
	public void tearDown() {
		/*
		 * This testing method doesn't contain any operation, just print
		 * something to the console showing it is called.
		 */
		//System.out.println("TestCase 24: PASS. MissionSequencer.tearDown() is executed.");
	}

    @Override
    @SCJAllowed(SUPPORT)
    public long immortalMemorySize() {
        // TODO Auto-generated method stub
        return 0;
    }

}
