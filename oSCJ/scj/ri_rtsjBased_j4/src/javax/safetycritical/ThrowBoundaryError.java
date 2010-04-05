package javax.safetycritical;

// (annotations turned off to work with Java 1.4) import static javax.safetycritical.annotate.Allocate.Area.CURRENT;

// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.Allocate;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.BlockFree;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// (annotations turned off to work with Java 1.4) @SCJAllowed
public class ThrowBoundaryError extends javax.realtime.ThrowBoundaryError {

// (annotations turned off to work with Java 1.4) 	@Allocate( { CURRENT })
// (annotations turned off to work with Java 1.4) 	@BlockFree
// (annotations turned off to work with Java 1.4) 	@SCJAllowed
	public ThrowBoundaryError() {
	}

// (annotations turned off to work with Java 1.4) 	@BlockFree
// (annotations turned off to work with Java 1.4) 	@SCJAllowed
	public String getPropagatedMessage() {
		return null;
	}

// (annotations turned off to work with Java 1.4) 	@Allocate( { CURRENT })
// (annotations turned off to work with Java 1.4) 	@BlockFree
// (annotations turned off to work with Java 1.4) 	@SCJAllowed
	public StackTraceElement[] getPropagatedStackTrace() {
		return null;
	}

// (annotations turned off to work with Java 1.4) 	@BlockFree
// (annotations turned off to work with Java 1.4) 	@SCJAllowed
	public int getPropagatedStackTraceDepth() {
		return 0;
	}

// (annotations turned off to work with Java 1.4) 	@BlockFree
// (annotations turned off to work with Java 1.4) 	@SCJAllowed
	public Class getPropagatedExceptionClass() {
		return null;
	}
}
