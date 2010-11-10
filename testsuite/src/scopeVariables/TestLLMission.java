package scopeVariables;

import static javax.safetycritical.annotate.Allocate.Area.THIS;

import javax.realtime.ImmortalMemory;
import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.realtime.ScopedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.CrossScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import copyInOut.LL;

@Scope("copyInOut.TestLLMission")
public class TestLLMission extends Mission {

	LL list;
	
	public LL getRealLL() {
		return this.list;
	}

	protected void initialize() {
		new MyHandler(null, null, null, 0, this);
	}

	@Override
	public long missionMemorySize() {
		return 0;
	}

	@Scope("copyInOut.TestLLMission")
	@RunsIn("copyInOut.MyHandler")
	class MyHandler extends PeriodicEventHandler {

		TestLLMission myMission;
		
		@Scope("copyInOut.TestLLMission") LL myLL;

		public MyHandler(PriorityParameters priority,
				PeriodicParameters parameters, StorageParameters scp,
				long memSize, TestLLMission mission) {
			super(priority, parameters, scp, memSize);
			this.myMission = mission;
		}

		public void handleEvent() {

			@Scope("copyInOut.TestLLMission") 
			LL realLL = myMission.getRealLL();					// OK

			this.myLL = realLL;									// OK!!
			
			LL localLL = realLL;								// ERROR!
			
			@Scope("copyInOut.TestLLMission")
			Mission mission = Mission.getCurrentMission(); 	// OK
															// mission can't be passed into any method, only into @CS method
															// all mission's method visible from here must be @CS
															// --> implicit or explicit inference, these limitations holds
			@Scope("Immortal")
			MemoryArea immMemory = ImmortalMemory.instance();  			// OK
			@Scope("copyInOut.TestLLMission")
			MemoryArea mem = RealtimeThread.getCurrentMemoryArea();     // OK
		}

		@Override
		public StorageParameters getThreadConfigurationParameters() {
			return null;
		}
	}
}
