package thruster;

import javax.safetycritical.MissionSequencer;
import javax.safetycritical.Safelet;

/**
 * This class is the Safelet for unit test. This unit test program is also an example level one
 * SCJ application.
 *
 * @author Lilei Zhai
 *
 */
public class MySafelet implements Safelet {

	public MySafelet() {
		System.out.println("Safelet being created");
	}

	/**
	 * This method returns the MissionSequencer of the application.
	 * "myMissionSequencer" takes a reference of the MissionSequencer. If
	 * "myMissionSequencer" is null, create a new MissionSequencer, otherwise
	 * return the "myMissionSequencer" directly.
	 */
	public MissionSequencer getSequencer() {
		System.out.println("TestCase 02: PASS. MissionSequencer.getSequencer() is executed.");
		return MyMissionSequencer.getInstance();
		//return null;
	}

	/**
	 * This method will be called before "getSequencer()". This method may
	 * create objects in ImmortalMemoryArea, set up hardware interrupt handlers,
	 * or initializing hardware devices.
	 */
	public void setUp() {
		/*
		 * This testing method doesn't contain any operation, just print
		 * something to the console showing it is called.
		 */
		System.out.println("TestCase 01: PASS. MissionSequencer.setUp() is executed.");
	}

	/**
	 * This method will be called after the termination of the MissionSequencer.
	 * It frees all resources used by the application.
	 */
	public void tearDown() {
		/*
		 * This testing method doesn't contain any operation, just print
		 * something to the console showing it is called.
		 */
		System.out.println("TestCase 24: PASS. MissionSequencer.tearDown() is executed.");
	}

}
