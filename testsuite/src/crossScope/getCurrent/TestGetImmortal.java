//scope/MyMission.java:64: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        mem.enterPrivateMemory(1000, new 
//                                     ^
//1 error

package crossScope.getCurrent;

import javax.realtime.ImmortalMemory;
import javax.realtime.MemoryArea;
import javax.realtime.RealtimeThread;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

@DefineScope(name = "my_area", parent = IMMORTAL)
class MemoryAreas {
	
	
}

@Scope(IMMORTAL)
public class TestGetImmortal {
	
    @RunsIn("my_area")
	public void method() {
		ImmortalMemory imm = ImmortalMemory.instance();
		
		MemoryArea mem = RealtimeThread.getCurrentMemoryArea();
		
		mem = imm;												// ERROR
		
	}
}
