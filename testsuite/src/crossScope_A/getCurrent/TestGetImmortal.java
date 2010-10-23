//scope/MyMission.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        mem.enterPrivateMemory(1000, new 
//                                     ^
//1 error

package crossScope_A.getCurrent;

import javax.realtime.ImmortalMemory;
import javax.realtime.MemoryArea;
import javax.realtime.RealtimeThread;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;


class MemoryAreas {
	
	@DefineScope(name = "my_area", parent = "immortal")
    PrivateMemory b = new PrivateMemory(0);
	
}

@Scope("immortal")
@RunsIn("my_area")
public class TestGetImmortal {
	
	public void method() {
		ImmortalMemory imm = ImmortalMemory.instance();
		
		MemoryArea mem = RealtimeThread.getCurrentMemoryArea();
		
		mem = imm;												// ERROR
		
	}

}
