//crossScope/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope.allocate;

import javax.safetycritical.Mission;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestMissingAlloc {

	
	
	@DefineScope(name = "Mission", parent = "Immortal")
    PrivateMemory mission = new PrivateMemory(0);

	
	public Mission returnMissionErr() {				//  ERROR @Allocate("mission") is missing
		Mission m = null;
		m = Mission.getCurrentMission();			
		
		return m;									//  ERROR @Allocate("mission") is missing, we are 
	}												//      loosing the scope information...
	
	
	
	
	@Scope("Mission")						// OK
	public Mission returnMission() {				
		Mission m = null;
		m = Mission.getCurrentMission();
		
		return m;									// OK
	}			
	
}
