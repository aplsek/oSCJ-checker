package javax.safetycritical;

// (annotations turned off to work with Java 1.4) import static javax.safetycritical.annotate.Level.LEVEL_0;

// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.BlockFree;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// @SCJAllowed(LEVEL_0) AJW THINKS THIS IS A DEAD CLASS
public class PriorityCeilingEmulation {
	// extends javax.realtime.PriorityCeilingEmulation {

// (annotations turned off to work with Java 1.4) 	@BlockFree
	// @SCJAllowed(LEVEL_0)
	public int getCeiling() {
		return 0;
	}

// (annotations turned off to work with Java 1.4) 	@BlockFree
	// @SCJAllowed(LEVEL_0)
	public static PriorityCeilingEmulation instance(int ceiling) {
		return null; // dummy return
	}

// (annotations turned off to work with Java 1.4) 	@BlockFree
// (annotations turned off to work with Java 1.4) 	@SCJAllowed(LEVEL_0)
	public static void setThreadDefaultCeiling(PriorityCeilingEmulation policy)
			throws IllegalStateException {
	}

// (annotations turned off to work with Java 1.4) 	@BlockFree
// (annotations turned off to work with Java 1.4) 	@SCJAllowed(LEVEL_0)
	public static void clearThreadDefaultCeiling() throws IllegalStateException {
	}

}
