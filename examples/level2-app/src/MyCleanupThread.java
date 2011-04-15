

import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;


import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedThread;
import javax.safetycritical.StorageParameters;

@SCJAllowed(members=true, value=LEVEL_2)
@Scope("PrimaryMission")
@DefineScope(name="MyCleanupThread", parent="PrimaryMission")
public class MyCleanupThread extends ManagedThread {

    @SCJRestricted(INITIALIZATION)
	public MyCleanupThread(PriorityParameters priority,
			StorageParameters storage) {
		super(priority, storage);
	}


	@Override
    @SCJAllowed(SUPPORT)
    @RunsIn("MyCleanupThread")
	public void run() {
		cleanupThis();
		cleanupThat();
	}

	@SCJAllowed
	@RunsIn("MyCleanupThread")
	void cleanupThis() {
		// code not shown
	}

	@SCJAllowed
	@RunsIn("MyCleanupThread")
	void cleanupThat() {
		// code not shown
	}

}
