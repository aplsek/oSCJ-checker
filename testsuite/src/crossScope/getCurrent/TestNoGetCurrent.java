//scope/MyMission.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        mem.enterPrivateMemory(1000, new 
//                                     ^
//1 error

package crossScope.getCurrent;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Scope.UNKNOWN;


/**
 * CrossScope methods can not call "getCurrent"
 * 
 * @author plsek
 */

@SCJAllowed
public class TestNoGetCurrent {

	@RunsIn(UNKNOWN)
	public void method(Clazz c) {
		Mission m = Mission.getCurrentMission(); 	// ERROR
	}

	class Clazz {
	}
}
