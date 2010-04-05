package javax.safetycritical;

import javax.safetycritical.AperiodicEventHandler;
// (annotations turned off to work with Java 1.4) import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.realtime.AsyncEvent;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.MemoryAreaEncloses;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// (annotations turned off to work with Java 1.4) @SCJAllowed(LEVEL_1)
public class ExternalEvent extends AsyncEvent {

	private String _happening;

// (annotations turned off to work with Java 1.4) 	@MemoryAreaEncloses(inner = { "this", "this" }, outer = { "handler",
//			"happening" })
// (annotations turned off to work with Java 1.4) 	@SCJAllowed(LEVEL_1)
	public ExternalEvent(AperiodicEventHandler handler, String happening) {
		_happening = happening;
		// TODO: should we add a copy of handler? Is the scope safety checked
		// statically ?
		addHandler(handler);
		MissionManager.getCurrentMissionManager().addExternalEvent(this);
	}

// (annotations turned off to work with Java 1.4) 	@MemoryAreaEncloses(inner = { "this", "this" }, outer = { "handlers",
//			"happening" })
// (annotations turned off to work with Java 1.4) 	@SCJAllowed(LEVEL_1)
	public ExternalEvent(AperiodicEventHandler[] handlers, String happening) {
		_happening = happening;
		
		/* Added by vhs for Java 1.4 support */
		for (int i = 0; i < handlers.length; i++) {
		    addHandler(handlers[i]);
		}

		/* Removed by vhs for Java 1.4 support
		for (AperiodicEventHandler handler : handlers)
			// TODO: same as above
			addHandler(handler);
		 */
		MissionManager.getCurrentMissionManager().addExternalEvent(this);
	}

// (annotations turned off to work with Java 1.4) 	@SCJAllowed(LEVEL_1)
	public void start() {
		bindTo(_happening);
	}

// (annotations turned off to work with Java 1.4) 	@SCJAllowed(LEVEL_1)
	public void cleanup() {
		unbindTo(_happening);
		setHandler(null);
	}
}
