package level2;

import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;
import javax.safetycritical.ManagedThread;
import javax.safetycritical.StorageParameters;

@SCJAllowed(members=true, value=LEVEL_2)
public class MyCleanupThread extends ManagedThread {

	public MyCleanupThread(PriorityParameters priority,
			StorageParameters storage) {
		super(priority, storage);
	}

	
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
