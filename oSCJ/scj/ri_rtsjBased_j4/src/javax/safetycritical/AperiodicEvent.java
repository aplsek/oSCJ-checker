package javax.safetycritical;

import javax.safetycritical.AperiodicEventHandler;
// (annotations turned off to work with Java 1.4) import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.realtime.AsyncEvent;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.Allocate;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.MemoryAreaEncloses;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.Allocate.Area;

// (annotations turned off to work with Java 1.4) @SCJAllowed(LEVEL_1)
public class AperiodicEvent extends AsyncEvent {

// (annotations turned off to work with Java 1.4) 	@Allocate( { Area.THIS })
// (annotations turned off to work with Java 1.4) 	@MemoryAreaEncloses(inner = { "this" }, outer = { "handler" })
// (annotations turned off to work with Java 1.4) 	@SCJAllowed(LEVEL_1)
	public AperiodicEvent(AperiodicEventHandler handler) {
		addHandler(handler);
		MissionManager.getCurrentMissionManager().addAperiodicEvent(this);
	}

// (annotations turned off to work with Java 1.4) 	@Allocate( { Area.THIS })
// (annotations turned off to work with Java 1.4) 	@MemoryAreaEncloses(inner = { "this" }, outer = { "handlers" })
// (annotations turned off to work with Java 1.4) 	@SCJAllowed(LEVEL_1)
	public AperiodicEvent(AperiodicEventHandler[] handlers) {
	    /* Added by vhs for Java 1.4 support */
	    for (int i = 0; i < handlers.length; i++) {
	        addHandler(handlers[i]);
	    }

	    /* Removed by vhs for Java 1.4 support
		for (AperiodicEventHandler handler : handlers)
			addHandler(handler);
		 */
	    
		MissionManager.getCurrentMissionManager().addAperiodicEvent(this);
	}

// (annotations turned off to work with Java 1.4) 	@SCJAllowed(LEVEL_1)
	public void fire() {
		super.fire();
	}

	void cleanup() {
		setHandler(null);
	}
}
