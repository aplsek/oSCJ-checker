package ovm.util;


/**
 * The base unchecked exception type for the OVM exception hierarchy.
 * Particular checked exceptions should be declared as nested types here
 * rather than using the standard exception types of the java.* packages.
 *
 * @author Krzysztof Palacz
 */
public class OVMRuntimeException extends RuntimeException {

    /**
     * Create a new <code>OVMRuntimeException</code> with an empty message 
     * and a filled in stack trace
     **/
    public OVMRuntimeException() {
    }
    
    /**
     * Creates a new <code>OVMRuntimeException</code> with an error message 
     * and a filled in stack trace
     * @param message the error message
     **/
    public OVMRuntimeException(String message) {
	super(message);
    }

    /**
     * Creates a new <code>OVMRuntimeException</code> with an error message
     * and a <code>Throwable</code> cause (when another error
     * causes this error), and a filled in stack trace
     * @param message the error message
     * @param cause the <code>Throwable</code> that caused this
     *              error
     **/    
    public OVMRuntimeException(String message, Throwable cause) {
	super(message);
	initCause(cause);
    }
    
    /**
     * Creates a new <code>OVMRuntimeException</code> with a 
     * <code>Throwable</code> cause (when another error
     * causes this error) and a filled in stack trace
     * @param cause the <code>Throwable</code> that caused this error
     **/
    public OVMRuntimeException(Throwable cause) {
	super(cause.getMessage());
	initCause(cause);
    }


    /**
     * Return a new <code>OVMRuntimeException</code> with the given message
     * that has the current <code>OVMRuntimeException</code> as its cause.
     */
    public OVMRuntimeException wrapped(String message) {
	return new OVMRuntimeException(message, this);
    }


    // NOTE: We do not have a printStacktrace(stream) method because we
    // should only be using the BasicIO mechanism under OVM, not any java.io
    // classes. We can't rewrite such methods in terms of BasicIO because
    // we compile against the JDK versions of the exception base classes,
    // not the Syslib versions.



    /** Illegal Access exception */
    public static class IllegalAccess extends OVMRuntimeException {
	public IllegalAccess(String message) {
	    super(message);
	}
	public IllegalAccess(Throwable cause) {
	    super(cause);
	}
    }
}
