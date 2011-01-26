package copyInOut;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

public class ManagedThreadExample {

}

class ManagedThread {

	//@MemoryAreaEncloses(inner = { "this", "this", "this" }, outer = {
	//		"schedule", "mem_info", "logic" })
	@SCJAllowed(LEVEL_2)
	@SCJRestricted(INITIALIZATION)
	public ManagedThread(PriorityParameters priority,
			StorageParameters mem_info, Runnable logic) {
		// ....
	}

}


@Scope(IMMORTAL)
class ImmortalEntry {
	
	MyManagedRunnable logic;
	PriorityParameters pp;
	StorageParameters mem;
	
}


@Scope("copyInOut.MyOtherMission2")
class MyOtherMission2 extends Mission {

	MyManagedRunnable logic;
	PriorityParameters pp;
	StorageParameters mem;
	
	public void initialize() {
		MyHandler handler = new MyHandler();
		
		ManagedThread mThread = new ManagedThread(pp, mem, logic);
	}

	@Override
	public long missionMemorySize() {
		return 0;
	}
}

@Scope("MyOtherMission2")
@RunsIn("MyHandler2")
class MyHandler2 extends PeriodicEventHandler {
	public MyHandler2() {
		super(null, null, null);
	}

	public MyHandler2(PriorityParameters priority, PeriodicParameters period,
			StorageParameters storage) {
		super(priority, period, storage);
	}

	@Scope(UNKNOWN)
	public LinkedList list;

	public void handleAsyncEvent() {
		// ....
	}

	@Override
	public StorageParameters getThreadConfigurationParameters() {
		return null;
	}
}


class MyManagedRunnable implements Runnable {
	public void run() {
		//....
	}
}