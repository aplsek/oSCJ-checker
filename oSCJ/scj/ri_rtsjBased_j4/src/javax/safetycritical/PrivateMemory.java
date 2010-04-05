package javax.safetycritical;

import javax.realtime.LTMemory;
import javax.realtime.MemoryArea;
import javax.realtime.RealtimeThread;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.BlockFree;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// (annotations turned off to work with Java 1.4) @SCJAllowed
public class PrivateMemory extends LTMemory implements ManagedMemory {

	private MissionManager _manager;

	private ManagedMemory _parent;

	private volatile boolean _occupied;

// (annotations turned off to work with Java 1.4) 	@SCJAllowed
	public PrivateMemory(long size) {
		super(size);
		// Creating "this" in non-managed memory will cause error. The validity
		// should be checked statically.
		MemoryArea mem = RealtimeThread.getCurrentMemoryArea();
		if (mem instanceof ManagedMemory)
			_parent = (ManagedMemory) mem;
		else
			throw new Error("Private memory cannot be created in " + mem);

		_manager = _parent.getManager();
	}

// (annotations turned off to work with Java 1.4) 	@BlockFree
// (annotations turned off to work with Java 1.4) 	@SCJAllowed
	public MissionManager getManager() {
		return _manager;
	}

// (annotations turned off to work with Java 1.4) 	@SCJAllowed
	public void enter(Runnable logic) {
		if (_parent != RealtimeThread.getCurrentMemoryArea())
			throw new Error("Attempt to enter private memory " + this
					+ " from the memory area other than its parent");
		if (_occupied)
			throw new Error("Attempt to enter the occupied private memory: "
					+ this);
		_occupied = true;
		super.enter(logic);
		_occupied = false;
	}
}
