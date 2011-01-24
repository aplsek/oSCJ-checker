package copyInOut;

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
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

import scopeVariables.LL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;


@Scope("copyInOut.TestLLMission")
public class TestLLMission extends Mission {

	LL ll;

	protected void initialize() {
		new MyHandler(null, null, null, 0, this);
	}

	@Override
	public long missionMemorySize() {
		return 0;
	}

	@RunsIn(UNKNOWN)
	public LL getLL() {
		return ll.copyDown();
	}

	public LL getRealLL() {
		return this.ll;
	}

	@RunsIn(UNKNOWN)
	public void putLL(LL h) { // ---> does not return a reference!!
		this.ll.copyUp(h);
	}

	@Scope("copyInOut.TestLLMission")
	@RunsIn("copyInOut.MyHandler")
	class MyHandler extends PeriodicEventHandler {

		TestLLMission myMission;

		public MyHandler(PriorityParameters priority,
				PeriodicParameters parameters, StorageParameters scp,
				long memSize, TestLLMission mission) {
			super(priority, parameters, scp);
			this.myMission = mission;
		}

		public void handleAsyncEvent() {

			LL myList = new LL();

			LL list = myMission.getLL();

			@Scope("copyInOut.TestLLMission") 
			LL realLL = myMission.getRealLL();

			// to copy from down to up???
			myMission.putLL(myList);

			@Scope("copyInOut.TestLLMission")
			Mission mission = Mission.getCurrentMission(); // where this lives?
															// mission can't be passed into any method, only into @CS method
															// all mission's method visible from here must be @CS
															// --> implicit or explicit inference, these limitations holds

			@Scope("Immortal")
			MemoryArea immMemory = ImmortalMemory.instance();
			@Scope("copyInOut.TestLLMission")
			MemoryArea mem = RealtimeThread.getCurrentMemoryArea();

			// MyRun run = new MyRun();
			// run.ll = myList;
			// run.myMission = myMission;
			// MemoryArea mem = MemoryArea.areaOf(myMission);
			// mem.executeInArea(run);

		}

		@RunsIn("MyMission")
		@Scope("Handler")
		class MyRun implements Runnable {

			LL ll;
			TestLLMission myMission;

			public void run() {
				myMission.putLL(ll.copyDown());
			}
		}

		@Override
		public StorageParameters getThreadConfigurationParameters() {
			return null;
		}
	}
}
