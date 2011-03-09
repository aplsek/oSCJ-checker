package scope.level2;

import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.safetycritical.ManagedThread;
import javax.safetycritical.StorageParameters;

@SCJAllowed(members=true, value=LEVEL_2)
public class MyCleanupThread extends ManagedThread {

    @SCJRestricted(INITIALIZATION)
	public MyCleanupThread(PriorityParameters priority,
			StorageParameters storage) {
		super(priority, storage);
	}


	@Override
    @SCJAllowed(SUPPORT)
	public void run() {
		cleanupThis();
		cleanupThat();
	}

	@SCJAllowed
	void cleanupThis() {
		// code not shown
	}

	@SCJAllowed
	void cleanupThat() {
		// code not shown
	}

}
